package mastermind.journal.eventstoredb

import arrow.core.*
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
    private val asEvent: ByteArray.(KClass<EVENT>) -> EVENT = createReader(),
    private val asBytes: EVENT.() -> ByteArray = createWriter()
) : Journal<EVENT, ERROR> {
    override suspend fun load(streamName: StreamName): Either<JournalError<ERROR>, LoadedStream<EVENT>> =
        try {
            eventStore.readStream(streamName)
                .mapToEvents(streamName)
        } catch (e: StreamNotFoundException) {
            StreamNotFound(streamName).left()
        }

    override suspend fun append(stream: UpdatedStream<EVENT>): Either<JournalError<ERROR>, LoadedStream<EVENT>> =
        try {
            eventStore.append(stream)
                .mapToLoadedStream(stream)
        } catch (e: WrongExpectedVersionException) {
            VersionConflict(
                stream.streamName,
                e.nextExpectedRevision.toRawLong() + 1,
                e.actualVersion.toRawLong() + 1
            ).left()
        }

    private suspend fun EventStoreDBClient.readStream(streamName: StreamName) =
        readStream(streamName, ReadStreamOptions.get()).await()

    private suspend fun EventStoreDBClient.append(stream: UpdatedStream<EVENT>): WriteResult =
        appendToStream(
            stream.streamName,
            AppendToStreamOptions.get().expectedRevision(stream.expectedRevision),
            stream.eventsToAppend.asBytesList().iterator()
        ).await()

    private fun ReadResult.mapToEvents(streamName: StreamName): Either<JournalError<ERROR>, LoadedStream<EVENT>> =
        events
            .map { resolvedEvent -> resolvedEvent.mapToEvent() }
            .toNonEmptyListOrNone()
            .map { events -> LoadedStream(streamName, lastStreamPosition + 1, events).right() }
            .getOrElse { StreamNotFound(streamName).left() }

    private fun ResolvedEvent.mapToEvent(): EVENT = event.eventData.asEvent(this.asClass())

    private fun ResolvedEvent.asClass(): KClass<EVENT> = Class.forName(event.eventType).kotlin as KClass<EVENT>

    private fun WriteResult.mapToLoadedStream(stream: UpdatedStream<EVENT>) = with(stream) {
        LoadedStream(
            streamName,
            nextExpectedRevision.toRawLong() + 1,
            events.toNonEmptyListOrNone()
                .map { it + eventsToAppend }
                .getOrElse { eventsToAppend }
        ).right()
    }

    private val UpdatedStream<EVENT>.expectedRevision: ExpectedRevision
        get() = if (streamVersion == 0L) ExpectedRevision.noStream() else ExpectedRevision.expectedRevision(
            streamVersion - 1
        )

    private fun NonEmptyList<EVENT>.asBytesList(): NonEmptyList<EventData> =
        map { event ->
            EventData.builderAsBinary(event::class.java.typeName, event.asBytes()).build()
        }
}