package mastermind.journal.eventstoredb

import arrow.core.*
import arrow.core.raise.either
import com.eventstore.dbclient.*
import kotlinx.coroutines.future.await
import mastermind.journal.Journal
import mastermind.journal.JournalError
import mastermind.journal.JournalError.StreamNotFound
import mastermind.journal.JournalError.VersionConflict
import mastermind.journal.Stream.LoadedStream
import mastermind.journal.Stream.UpdatedStream
import mastermind.journal.StreamName
import kotlin.reflect.KClass

class EventStoreDbJournal<EVENT : Any, ERROR : Any>(
    private val eventStore: EventStoreDBClient,
    private val asEvent: ByteArray.(KClass<EVENT>) -> Either<ReadError, EVENT> = createReader(),
    private val asBytes: EVENT.() -> Either<WriteError, ByteArray> = createWriter()
) : Journal<EVENT, ERROR> {
    override suspend fun load(streamName: StreamName): Either<JournalError<ERROR>, LoadedStream<EVENT>> =
        try {
            eventStore.readStream(streamName)
                .mapToEvents(streamName)
        } catch (e: StreamNotFoundException) {
            StreamNotFound<ERROR>(streamName).left()
        }

    override suspend fun append(stream: UpdatedStream<EVENT>): Either<JournalError<ERROR>, LoadedStream<EVENT>> =
        with(stream) {
            eventsToAppend
                .asBytesList()
                .append()
        }

    private fun ReadResult.mapToEvents(streamName: StreamName): Either<JournalError<ERROR>, LoadedStream<EVENT>> =
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
            .getOrNull()?.getOrNull()?.right() ?: StreamNotFound<ERROR>(streamName).left()

    private fun ResolvedEvent.asClass(): KClass<EVENT> = Class.forName(event.eventType).kotlin as KClass<EVENT>

    private suspend fun EventStoreDBClient.readStream(streamName: StreamName) =
        readStream(streamName, ReadStreamOptions.get()).await()

    context(UpdatedStream<EVENT>)
    private fun NonEmptyList<EVENT>.asBytesList(): Either<JournalError<ERROR>, NonEmptyList<EventData>> =
        either {
            map { event ->
                event
                    .asBytes()
                    .map { event::class.java.typeName to it }
                    .map { (type, bytes) -> EventData.builderAsBinary(type, bytes).build() }
                    .mapLeft<JournalError<ERROR>> { _ -> StreamNotFound(streamName) }
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
    private suspend fun Either<JournalError<ERROR>, NonEmptyList<EventData>>.append(): Either<JournalError<ERROR>, LoadedStream<EVENT>> =
        try {
            map { events -> eventStore.append(events).mapToLoadedStream() }
        } catch (e: WrongExpectedVersionException) {
            VersionConflict<ERROR>(
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