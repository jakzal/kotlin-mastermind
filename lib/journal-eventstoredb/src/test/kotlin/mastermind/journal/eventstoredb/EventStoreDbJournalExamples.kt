package mastermind.journal.eventstoredb

import arrow.core.*
import arrow.core.raise.either
import com.eventstore.dbclient.*
import kotlinx.coroutines.future.await
import mastermind.journal.*
import mastermind.journal.JournalFailure.EventStoreFailure
import mastermind.journal.JournalFailure.EventStoreFailure.StreamNotFound
import mastermind.journal.JournalFailure.EventStoreFailure.VersionConflict
import mastermind.journal.JournalFailure.ExecutionFailure
import mastermind.journal.Stream.*
import mastermind.testkit.testcontainers.eventstoredb.EventStoreDbContainer
import org.junit.jupiter.api.Tag
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import kotlin.reflect.KClass

@Testcontainers(disabledWithoutDocker = true)
@Tag("io")
class EventStoreDbJournalExamples : JournalContract() {
    private val journal = EventStoreDbJournal<TestEvent, TestFailure>(eventStoreClient())

    companion object {
        @Container
        private val eventStoreDb: EventStoreDbContainer<*> = EventStoreDbContainer()
    }

    override fun journal(): Journal<TestEvent, TestFailure> = journal

    override suspend fun loadEvents(streamName: StreamName): List<TestEvent> =
        journal.load(streamName).fold({ emptyList() }, { it.events })

    private fun eventStoreClient(): EventStoreDBClient = EventStoreDBClient.create(
        EventStoreDBConnectionString.parseOrThrow(eventStoreDb.connectionString)
    )
}

class EventStoreDbJournal<EVENT : Any, FAILURE : Any>(
    private val eventStore: EventStoreDBClient,
    private val asEvent: ByteArray.(KClass<EVENT>) -> Either<ReadFailure, EVENT> = createReader(),
    private val asBytes: EVENT.() -> Either<WriteFailure, ByteArray> = createWriter()
) : Journal<EVENT, FAILURE> {
    override suspend fun stream(
        streamName: StreamName,
        onStream: Stream<EVENT>.() -> Either<FAILURE, UpdatedStream<EVENT>>
    ): Either<JournalFailure<FAILURE>, LoadedStream<EVENT>> = try {
        load(streamName)
            .orCreate(streamName)
            .execute(onStream)
            .append()
    } catch (e: WrongExpectedVersionException) {
        VersionConflict<FAILURE>(streamName).left()
    }

    override suspend fun load(streamName: StreamName): Either<EventStoreFailure<FAILURE>, LoadedStream<EVENT>> =
        try {
            eventStore.readStream(streamName)
                .mapToEvents(streamName)
        } catch (e: StreamNotFoundException) {
            StreamNotFound<FAILURE>(streamName).left()
        }

    private fun Either<EventStoreFailure<FAILURE>, LoadedStream<EVENT>>.orCreate(streamName: StreamName): Either<JournalFailure<FAILURE>, Stream<EVENT>> =
        recover<JournalFailure<FAILURE>, JournalFailure<FAILURE>, Stream<EVENT>> { e ->
            if (e is StreamNotFound) EmptyStream(streamName)
            else raise(e)
        }

    private fun Either<JournalFailure<FAILURE>, Stream<EVENT>>.execute(onStream: Stream<EVENT>.() -> Either<FAILURE, UpdatedStream<EVENT>>): Either<JournalFailure<FAILURE>, UpdatedStream<EVENT>> =
        flatMap { stream -> stream.onStream().mapLeft(::ExecutionFailure) }

    context(UpdatedStream<EVENT>)
    private fun NonEmptyList<EVENT>.asBytesList(): Either<JournalFailure<FAILURE>, NonEmptyList<EventData>> = either {
        map { event ->
            event
                .asBytes()
                .map { event::class.java.typeName to it }
                .map { (type, bytes) -> EventData.builderAsBinary(type, bytes).build() }
                .mapLeft<JournalFailure<FAILURE>> { _ -> StreamNotFound(this@UpdatedStream.streamName) }
        }.bindAll()
    }

    private suspend fun Either<JournalFailure<FAILURE>, UpdatedStream<EVENT>>.append(): Either<JournalFailure<FAILURE>, LoadedStream<EVENT>> {
        return flatMap { stream ->
            with(stream) {
                eventsToAppend
                    .asBytesList()
                    .append()
            }
        }
    }

    context(UpdatedStream<EVENT>)
    private fun WriteResult.mapToLoadedStream() = LoadedStream(
        streamName,
        nextExpectedRevision.toRawLong() + 1,
        events.toNonEmptyListOrNone()
            .map { it + eventsToAppend }
            .getOrElse { eventsToAppend }
    )

    context(UpdatedStream<EVENT>)
    private suspend fun Either<JournalFailure<FAILURE>, NonEmptyList<EventData>>.append(): Either<JournalFailure<FAILURE>, LoadedStream<EVENT>> =
        map { events -> eventStore.append(events).mapToLoadedStream() }.mapLeft { _ -> StreamNotFound(streamName) }

    context(UpdatedStream<EVENT>)
    private suspend fun EventStoreDBClient.append(events: NonEmptyList<EventData>): WriteResult =
        appendToStream(
            streamName,
            AppendToStreamOptions.get().expectedRevision(expectedRevision),
            events.iterator()
        ).await()

    private suspend fun EventStoreDBClient.readStream(streamName: StreamName) =
        readStream(streamName, ReadStreamOptions.get()).await()

    private val UpdatedStream<EVENT>.expectedRevision: ExpectedRevision
        get() = if (streamVersion == 0L) ExpectedRevision.noStream() else ExpectedRevision.expectedRevision(
            streamVersion - 1
        )

    private fun ReadResult.mapToEvents(streamName: StreamName): Either<EventStoreFailure<FAILURE>, LoadedStream<EVENT>> =
        events
            .mapOrAccumulate { resolvedEvent ->
                resolvedEvent
                    .event
                    .eventData
                    .asEvent(Class.forName(resolvedEvent.event.eventType).kotlin as KClass<EVENT>)
                    .bind()
            }
            .map { events ->
                events
                    .toNonEmptyListOrNone()
                    .map { e -> LoadedStream(streamName, lastStreamPosition + 1, e) }
            }
            .getOrNull()?.getOrNull()?.right() ?: StreamNotFound<FAILURE>(streamName).left()
}
