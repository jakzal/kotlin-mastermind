package mastermind.journal.eventstoredb

import arrow.core.*
import com.eventstore.dbclient.*
import kotlinx.coroutines.future.await
import mastermind.journal.Entries.LoadedEntries
import mastermind.journal.Entries.UpdatedEntries
import mastermind.journal.JournalCatalogue
import mastermind.journal.JournalError
import mastermind.journal.JournalError.StreamNotFound
import mastermind.journal.JournalError.VersionConflict
import mastermind.journal.JournalName
import kotlin.reflect.KClass

class EventStoreDbJournalCatalogue<ENTRY : Any, ERROR : Any>(
    private val eventStore: EventStoreDBClient,
    private val asEvent: ByteArray.(KClass<ENTRY>) -> ENTRY = createReader(),
    private val asBytes: ENTRY.() -> ByteArray = createWriter()
) : JournalCatalogue<ENTRY, ERROR> {
    constructor(connectionString: String) : this(EventStoreDBClient.create(
        EventStoreDBConnectionString.parseOrThrow(connectionString)
    ))

    override suspend fun load(journalName: JournalName): Either<JournalError<ERROR>, LoadedEntries<ENTRY>> =
        try {
            eventStore.readStream(journalName)
                .mapToEvents(journalName)
        } catch (e: StreamNotFoundException) {
            StreamNotFound(journalName).left()
        }

    override suspend fun append(stream: UpdatedEntries<ENTRY>): Either<JournalError<ERROR>, LoadedEntries<ENTRY>> =
        try {
            eventStore.append(stream)
                .mapToLoadedStream(stream)
        } catch (e: WrongExpectedVersionException) {
            VersionConflict(
                stream.journalName,
                e.nextExpectedRevision.toRawLong() + 1,
                e.actualVersion.toRawLong() + 1
            ).left()
        }

    private suspend fun EventStoreDBClient.readStream(journalName: JournalName) =
        readStream(journalName, ReadStreamOptions.get()).await()

    private suspend fun EventStoreDBClient.append(stream: UpdatedEntries<ENTRY>): WriteResult =
        appendToStream(
            stream.journalName,
            AppendToStreamOptions.get().expectedRevision(stream.expectedRevision),
            stream.entriesToAppend.asBytesList().iterator()
        ).await()

    private fun ReadResult.mapToEvents(journalName: JournalName): Either<JournalError<ERROR>, LoadedEntries<ENTRY>> =
        events
            .map { resolvedEvent -> resolvedEvent.mapToEvent() }
            .toNonEmptyListOrNone()
            .map { events -> LoadedEntries(journalName, lastStreamPosition + 1, events).right() }
            .getOrElse { StreamNotFound(journalName).left() }

    private fun ResolvedEvent.mapToEvent(): ENTRY = event.eventData.asEvent(this.asClass())

    private fun ResolvedEvent.asClass(): KClass<ENTRY> = Class.forName(event.eventType).kotlin as KClass<ENTRY>

    private fun WriteResult.mapToLoadedStream(stream: UpdatedEntries<ENTRY>) = with(stream) {
        LoadedEntries(
            journalName,
            nextExpectedRevision.toRawLong() + 1,
            entries.toNonEmptyListOrNone()
                .map { it + entriesToAppend }
                .getOrElse { entriesToAppend }
        ).right()
    }

    private val UpdatedEntries<ENTRY>.expectedRevision: ExpectedRevision
        get() = if (journalVersion == 0L) ExpectedRevision.noStream() else ExpectedRevision.expectedRevision(
            journalVersion - 1
        )

    private fun NonEmptyList<ENTRY>.asBytesList(): NonEmptyList<EventData> =
        map { event ->
            EventData.builderAsBinary(event::class.java.typeName, event.asBytes()).build()
        }
}