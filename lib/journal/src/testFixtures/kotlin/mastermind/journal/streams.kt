package mastermind.journal

import arrow.core.NonEmptyList
import arrow.core.nonEmptyListOf
import mastermind.journal.Entries.*

fun <ENTRY : Any> loadedEntries(journalName: JournalName, entries: NonEmptyList<ENTRY>): LoadedEntries<ENTRY> =
    LoadedEntries(journalName, entries.size.toLong(), entries)

fun <ENTRY : Any> loadedEntries(journalName: JournalName, entry: ENTRY, vararg entries: ENTRY): LoadedEntries<ENTRY> =
    loadedEntries(journalName, nonEmptyListOf(entry, *entries))

fun <ENTRY : Any> updatedEntries(
    journalName: JournalName,
    entry: ENTRY,
    vararg entries: ENTRY
): UpdatedEntries<ENTRY> =
    updatedEntries(EmptyEntries(journalName), entry, *entries)

fun <ENTRY : Any> updatedEntries(
    existingEntries: Entries<ENTRY>,
    entry: ENTRY,
    vararg entries: ENTRY
): UpdatedEntries<ENTRY> =
    existingEntries.append<ENTRY, Nothing>(entry, *entries).getOrNull()
        ?: throw RuntimeException("Failed to create an updated stream.")
