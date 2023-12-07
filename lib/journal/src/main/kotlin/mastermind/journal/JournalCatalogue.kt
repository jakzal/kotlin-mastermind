package mastermind.journal

import arrow.core.Either
import mastermind.journal.Entries.LoadedEntries
import mastermind.journal.Entries.UpdatedEntries

interface JournalCatalogue<ENTRY : Any, ERROR : Any> {
    suspend fun load(journalName: JournalName): Either<JournalError<ERROR>, LoadedEntries<ENTRY>>
    suspend fun append(stream: UpdatedEntries<ENTRY>): Either<JournalError<ERROR>, LoadedEntries<ENTRY>>
}