package mastermind.journal

import arrow.core.Either
import arrow.core.NonEmptyList
import arrow.core.flatMap
import arrow.core.recover
import mastermind.journal.JournalError.ExecutionError
import mastermind.journal.JournalError.StreamNotFound
import mastermind.journal.Stream.*

private typealias LoadedStreamOutcome<ERROR, EVENT> = Either<JournalError<ERROR>, LoadedStream<EVENT>>
private typealias StreamOutcome<ERROR, EVENT> = Either<JournalError<ERROR>, Stream<EVENT>>
private typealias UpdatedStreamOutcome<ERROR, EVENT> = Either<JournalError<ERROR>, UpdatedStream<EVENT>>

context(Journal<EVENT, ERROR>)
suspend fun <EVENT : Any, ERROR : Any> loadToAppend(
    streamName: StreamName,
    onEvents: (List<EVENT>) -> Either<ERROR, NonEmptyList<EVENT>>
): Either<JournalError<ERROR>, NonEmptyList<EVENT>> =
    load(streamName)
        .orCreate(streamName)
        .update(onEvents)
        .persist()
        .map(LoadedStream<EVENT>::events)

private fun <EVENT : Any, ERROR : Any> LoadedStreamOutcome<ERROR, EVENT>.orCreate(
    streamName: StreamName
): StreamOutcome<ERROR, EVENT> =
    recover { e ->
        if (e is StreamNotFound) EmptyStream(streamName)
        else raise(e)
    }

private fun <EVENT : Any, ERROR : Any> StreamOutcome<ERROR, EVENT>.update(
    onEvents: (List<EVENT>) -> Either<ERROR, NonEmptyList<EVENT>>
): UpdatedStreamOutcome<ERROR, EVENT> =
    flatMap { stream ->
        stream
            .append { onEvents(stream.events) }
            .mapLeft(::ExecutionError)
    }

context(Journal<EVENT, ERROR>)
private suspend fun <EVENT : Any, ERROR : Any> UpdatedStreamOutcome<ERROR, EVENT>.persist()
        : LoadedStreamOutcome<ERROR, EVENT> =
    flatMap { streamToWrite -> append(streamToWrite) }
