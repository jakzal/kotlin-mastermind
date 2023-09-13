package mastermind.journal.eventstoredb

import arrow.core.*
import com.eventstore.dbclient.*
import kotlinx.coroutines.future.await
import mastermind.journal.*
import mastermind.journal.JournalFailure.EventStoreFailure
import mastermind.journal.JournalFailure.EventStoreFailure.StreamNotFound
import mastermind.journal.Stream.EmptyStream
import mastermind.journal.Stream.LoadedStream
import mastermind.testkit.testcontainers.eventstoredb.EventStoreDbContainer
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Tag
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import kotlin.reflect.KClass

@Testcontainers(disabledWithoutDocker = true)
@Tag("io")
@Disabled
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
    override suspend fun <FAILURE : Any> stream(
        streamName: StreamName,
        onStream: Stream<EVENT>.() -> Either<FAILURE, Stream.UpdatedStream<EVENT>>
    ): Either<JournalFailure<FAILURE>, LoadedStream<EVENT>> {
        return load(streamName)
            .orCreate(streamName)
            .flatMap(onStream)
            .flatMap { stream ->
                stream.eventsToAppend
                    .mapOrAccumulate { event ->
                        event
                            .asBytes()
                            .map { event::class.java.typeName to it }
                            .map { (type, bytes) -> EventData.builderAsBinary(type, bytes).build() }
                            .bind()
                    }
                    .map { events ->
                        eventStore
                            .appendToStream(
                                streamName,
                                AppendToStreamOptions.get().expectedRevision(
                                    if (stream.streamVersion == 0L) ExpectedRevision.noStream() else ExpectedRevision.expectedRevision(
                                        stream.streamVersion - 1
                                    )
                                ),
                                events.iterator()
                            )
                            .await()
                            .let { writeResult ->
                                LoadedStream(
                                    streamName,
                                    writeResult.nextExpectedRevision.toRawLong() + 1,
                                    stream.events.toNonEmptyListOrNone()
                                        .map { it + stream.eventsToAppend }
                                        .getOrElse { stream.eventsToAppend }
                                )
                            }
                    }
                    .also { println(it) }
                    .mapLeft { _ -> StreamNotFound<FAILURE>(streamName) }
            } as Either<JournalFailure<FAILURE>, LoadedStream<EVENT>>
    }

    override suspend fun load(streamName: StreamName): Either<EventStoreFailure<FAILURE>, LoadedStream<EVENT>> {
        return try {
            eventStore.readStream(streamName, ReadStreamOptions.get())
                .await()
                .let { readResult ->
                    readResult.events
                        .mapOrAccumulate { resolvedEvent ->
                            resolvedEvent
                                .event
                                .eventData
                                .asEvent(Class.forName(resolvedEvent.event.eventType).kotlin as KClass<EVENT>)
                                .bind()
                        }
                        .also { println(it) }
                        .mapLeft { _ -> StreamNotFound<FAILURE>(streamName) }
                        .map { events ->
                            events
                                .toNonEmptyListOrNone()
                                .map { e -> LoadedStream(streamName, readResult.lastStreamPosition + 1, e) }
                        }
                        .getOrNull()?.getOrNull()?.right() ?: StreamNotFound<FAILURE>(streamName).left()
                }
        } catch (e: StreamNotFoundException) {
            StreamNotFound<FAILURE>(streamName).left()
        }
    }

    private fun Either<EventStoreFailure<FAILURE>, LoadedStream<EVENT>>.orCreate(streamName: StreamName): Either<JournalFailure<FAILURE>, Stream<EVENT>> =
        recover<JournalFailure<FAILURE>, JournalFailure<FAILURE>, Stream<EVENT>> { e ->
            if (e is StreamNotFound) EmptyStream(streamName)
            else raise(e)
        }
}
