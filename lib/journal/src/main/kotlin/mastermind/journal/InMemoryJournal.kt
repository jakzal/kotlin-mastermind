package mastermind.journal

import arrow.atomic.Atomic
import arrow.core.Either
import arrow.core.NonEmptyList
import arrow.core.getOrElse
import arrow.core.raise.either
import arrow.core.toNonEmptyListOrNone
import mastermind.journal.Entries.LoadedEntries
import mastermind.journal.Entries.UpdatedEntries
import mastermind.journal.JournalError.StreamNotFound

class InMemoryJournal<ENTRY : Any, ERROR : Any>(
    private val events: Atomic<Map<JournalName, LoadedEntries<ENTRY>>> = Atomic(mapOf())
) : Journal<ENTRY, ERROR> {
    override suspend fun load(journalName: JournalName): Either<JournalError<ERROR>, LoadedEntries<ENTRY>> = either {
        events.get()[journalName] ?: raise(StreamNotFound(journalName))
    }

    override suspend fun append(stream: UpdatedEntries<ENTRY>): Either<JournalError<ERROR>, LoadedEntries<ENTRY>> =
        either {
            events.updateAndGet {
                it[stream.journalName]?.let { writtenStream ->
                    if (writtenStream.journalVersion != stream.journalVersion) {
                        raise(
                            JournalError.VersionConflict(
                                stream.journalName,
                                stream.journalVersion,
                                writtenStream.journalVersion
                            )
                        )
                    }
                }
                it + mapOf(stream.journalName to stream.toLoadedStream())
            }[stream.journalName] ?: raise(StreamNotFound(stream.journalName))
        }
}

private fun <ENTRY : Any> UpdatedEntries<ENTRY>.toLoadedStream(): LoadedEntries<ENTRY> =
    LoadedEntries(journalName, journalVersion + entriesToAppend.size, entries + entriesToAppend)

private infix operator fun <T> List<T>.plus(other: NonEmptyList<T>): NonEmptyList<T> =
    toNonEmptyListOrNone()
        .map { items -> items + other }
        .getOrElse { other }
