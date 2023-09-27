package mastermind.journal.eventstoredb

import arrow.core.*
import arrow.core.raise.either
import com.eventstore.dbclient.*
import kotlinx.coroutines.future.await
import mastermind.journal.EventStore
import mastermind.journal.JournalFailure.EventStoreFailure
import mastermind.journal.JournalFailure.EventStoreFailure.StreamNotFound
import mastermind.journal.JournalFailure.EventStoreFailure.VersionConflict
import mastermind.journal.Stream.LoadedStream
import mastermind.journal.Stream.UpdatedStream
import mastermind.journal.StreamName
import kotlin.reflect.KClass

class EventStoreDb<EVENT : Any, FAILURE : Any>(
    private val eventStore: EventStoreDBClient,
    private val asEvent: ByteArray.(KClass<EVENT>) -> Either<ReadFailure, EVENT> = createReader(),
    private val asBytes: EVENT.() -> Either<WriteFailure, ByteArray> = createWriter()
) : EventStore<EVENT, FAILURE> {
    override suspend fun load(streamName: StreamName): Either<EventStoreFailure<FAILURE>, LoadedStream<EVENT>> =
        try {
            eventStore.readStream(streamName)
                .mapToEvents(streamName)
        } catch (e: StreamNotFoundException) {
            StreamNotFound<FAILURE>(streamName).left()
        }

    override suspend fun append(stream: UpdatedStream<EVENT>): Either<EventStoreFailure<FAILURE>, LoadedStream<EVENT>> =
        with(stream) {
            eventsToAppend
                .asBytesList()
                .append()
        }

    private fun ReadResult.mapToEvents(streamName: StreamName): Either<EventStoreFailure<FAILURE>, LoadedStream<EVENT>> =
        events
            .mapOrAccumulate { resolvedEvent ->
                resolvedEvent
                    .event
                    .eventData
                    .asEvent(resolvedEvent.asClass())
                    .bind()
            }
            .map { events ->
                events
                    .toNonEmptyListOrNone()
                    .map { e -> LoadedStream(streamName, lastStreamPosition + 1, e) }
            }
            .getOrNull()?.getOrNull()?.right() ?: StreamNotFound<FAILURE>(streamName).left()

    private fun ResolvedEvent.asClass(): KClass<EVENT> = Class.forName(event.eventType).kotlin as KClass<EVENT>

    private suspend fun EventStoreDBClient.readStream(streamName: StreamName) =
        readStream(streamName, ReadStreamOptions.get()).await()

    context(UpdatedStream<EVENT>)
    private fun NonEmptyList<EVENT>.asBytesList(): Either<EventStoreFailure<FAILURE>, NonEmptyList<EventData>> =
        either {
            map { event ->
                event
                    .asBytes()
                    .map { event::class.java.typeName to it }
                    .map { (type, bytes) -> EventData.builderAsBinary(type, bytes).build() }
                    .mapLeft<EventStoreFailure<FAILURE>> { _ -> StreamNotFound(streamName) }
            }.bindAll()
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
    private suspend fun Either<EventStoreFailure<FAILURE>, NonEmptyList<EventData>>.append(): Either<EventStoreFailure<FAILURE>, LoadedStream<EVENT>> =
        try {
            map { events -> eventStore.append(events).mapToLoadedStream() }
        } catch (e: WrongExpectedVersionException) {
            VersionConflict<FAILURE>(
                streamName,
                e.nextExpectedRevision.toRawLong() + 1,
                e.actualVersion.toRawLong() + 1
            ).left()
        }

    context(UpdatedStream<EVENT>)
    private suspend fun EventStoreDBClient.append(events: NonEmptyList<EventData>): WriteResult =
        appendToStream(
            streamName,
            AppendToStreamOptions.get().expectedRevision(expectedRevision),
            events.iterator()
        ).await()

    private val UpdatedStream<EVENT>.expectedRevision: ExpectedRevision
        get() = if (streamVersion == 0L) ExpectedRevision.noStream() else ExpectedRevision.expectedRevision(
            streamVersion - 1
        )
}