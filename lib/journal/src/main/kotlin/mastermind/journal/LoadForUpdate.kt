package mastermind.journal

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.recover
import mastermind.journal.JournalError.ExecutionError
import mastermind.journal.Stream.LoadedStream
import mastermind.journal.Stream.UpdatedStream

context(Journal<EVENT, ERROR>)
suspend fun <EVENT : Any, ERROR : Any> loadForUpdate(
    streamName: StreamName,
    onStream: Stream<EVENT>.() -> Either<ERROR, UpdatedStream<EVENT>>
): Either<JournalError<ERROR>, LoadedStream<EVENT>> =
    load(streamName)
        .orCreate(streamName)
        .execute(onStream)
        .append()

private fun <EVENT : Any, ERROR : Any> Either<JournalError<ERROR>, LoadedStream<EVENT>>.orCreate(
    streamName: StreamName
): Either<JournalError<ERROR>, Stream<EVENT>> =
    recover { e ->
        if (e is JournalError.StreamNotFound) Stream.EmptyStream(streamName)
        else raise(e)
    }

private fun <EVENT : Any, ERROR : Any> Either<JournalError<ERROR>, Stream<EVENT>>.execute(
    onStream: Stream<EVENT>.() -> Either<ERROR, UpdatedStream<EVENT>>
): Either<JournalError<ERROR>, UpdatedStream<EVENT>> =
    flatMap { stream -> stream.onStream().mapLeft(::ExecutionError) }

context(Journal<EVENT, ERROR>)
private suspend fun <EVENT : Any, ERROR : Any> Either<JournalError<ERROR>, UpdatedStream<EVENT>>.append()
        : Either<JournalError<ERROR>, LoadedStream<EVENT>> =
    flatMap { streamToWrite ->
        append(streamToWrite)
    }
