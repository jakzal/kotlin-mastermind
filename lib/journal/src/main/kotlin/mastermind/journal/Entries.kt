package mastermind.journal

import arrow.core.*
import mastermind.journal.Entries.UpdatedEntries

typealias JournalName = String
typealias JournalVersion = Long

sealed interface Entries<ENTRY : Any> {
    val journalName: JournalName
    val journalVersion: JournalVersion
    val entries: List<ENTRY>

    data class EmptyEntries<ENTRY : Any>(override val journalName: JournalName) : Entries<ENTRY> {
        override val journalVersion: JournalVersion = 0
        override val entries: List<ENTRY> = emptyList()
    }

    data class LoadedEntries<ENTRY : Any>(
        override val journalName: JournalName,
        override val journalVersion: JournalVersion,
        override val entries: NonEmptyList<ENTRY>
    ) : Entries<ENTRY>

    data class UpdatedEntries<ENTRY : Any>(
        override val journalName: JournalName,
        override val journalVersion: JournalVersion,
        override val entries: List<ENTRY>,
        val entriesToAppend: NonEmptyList<ENTRY>
    ) : Entries<ENTRY>
}

fun <ENTRY : Any, ERROR : Any> Entries<ENTRY>.append(
    generateEvents: () -> Either<ERROR, NonEmptyList<ENTRY>>
): Either<ERROR, UpdatedEntries<ENTRY>> =
    generateEvents().flatMap(::append)

fun <ENTRY : Any, ERROR : Any> Entries<ENTRY>.append(
    event: ENTRY,
    vararg events: ENTRY
): Either<ERROR, UpdatedEntries<ENTRY>> =
    append(nonEmptyListOf(event, *events))

private fun <ENTRY : Any, ERROR : Any> Entries<ENTRY>.append(eventsToAppend: NonEmptyList<ENTRY>): Either<ERROR, UpdatedEntries<ENTRY>> =
    UpdatedEntries(journalName, journalVersion, entries, eventsToAppend).right()
