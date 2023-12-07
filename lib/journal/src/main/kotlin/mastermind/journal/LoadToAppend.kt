package mastermind.journal

import arrow.core.Either
import arrow.core.NonEmptyList
import arrow.core.flatMap
import arrow.core.recover
import mastermind.journal.Entries.*
import mastermind.journal.JournalError.ExecutionError
import mastermind.journal.JournalError.StreamNotFound

private typealias LoadedStreamOutcome<ERROR, ENTRY> = Either<JournalError<ERROR>, LoadedEntries<ENTRY>>
private typealias StreamOutcome<ERROR, ENTRY> = Either<JournalError<ERROR>, Entries<ENTRY>>
private typealias UpdatedStreamOutcome<ERROR, ENTRY> = Either<JournalError<ERROR>, UpdatedEntries<ENTRY>>

context(Journal<ENTRY, ERROR>)
suspend fun <ENTRY : Any, ERROR : Any> loadToAppend(
    journalName: JournalName,
    onEntries: (List<ENTRY>) -> Either<ERROR, NonEmptyList<ENTRY>>
): Either<JournalError<ERROR>, NonEmptyList<ENTRY>> =
    load(journalName)
        .orCreate(journalName)
        .update(onEntries)
        .persist()
        .map(LoadedEntries<ENTRY>::entries)

private fun <ENTRY : Any, ERROR : Any> LoadedStreamOutcome<ERROR, ENTRY>.orCreate(
    journalName: JournalName
): StreamOutcome<ERROR, ENTRY> =
    recover { e ->
        if (e is StreamNotFound) EmptyEntries(journalName)
        else raise(e)
    }

private fun <ENTRY : Any, ERROR : Any> StreamOutcome<ERROR, ENTRY>.update(
    onEvents: (List<ENTRY>) -> Either<ERROR, NonEmptyList<ENTRY>>
): UpdatedStreamOutcome<ERROR, ENTRY> =
    flatMap { stream ->
        stream
            .append { onEvents(stream.entries) }
            .mapLeft(::ExecutionError)
    }

context(Journal<ENTRY, ERROR>)
private suspend fun <ENTRY : Any, ERROR : Any> UpdatedStreamOutcome<ERROR, ENTRY>.persist()
        : LoadedStreamOutcome<ERROR, ENTRY> =
    flatMap { streamToWrite -> append(streamToWrite) }
