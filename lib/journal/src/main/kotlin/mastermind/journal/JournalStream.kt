package mastermind.journal

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.recover
import mastermind.journal.JournalError.StreamNotFound
import mastermind.journal.Stream.*

context(Journal<EVENT, ERROR>)
fun <EVENT : Any, ERROR : Any> createUpdateStream(): UpdateStream<EVENT, JournalError<ERROR>> = with(this@Journal) {
    { streamName, onStream ->
        load(streamName)
            .orCreate(streamName)
            .execute(onStream)
            .append()
    }
}

context(Journal<EVENT, ERROR>)
fun <EVENT : Any, ERROR : Any> createLoadStream(): LoadStream<EVENT, JournalError<ERROR>> = { streamName ->
    load(streamName)
}

private fun <EVENT : Any, ERROR : Any> Either<JournalError<ERROR>, LoadedStream<EVENT>>.orCreate(streamName: StreamName): Either<JournalError<ERROR>, Stream<EVENT>> =
    recover { e ->
        if (e is StreamNotFound) EmptyStream(streamName)
        else raise(e)
    }

private fun <EVENT : Any, ERROR : Any> Either<JournalError<ERROR>, Stream<EVENT>>.execute(onStream: Stream<EVENT>.() -> Either<JournalError<ERROR>, UpdatedStream<EVENT>>): Either<JournalError<ERROR>, UpdatedStream<EVENT>> =
    flatMap { stream -> stream.onStream() }

context(Journal<EVENT, ERROR>)
private suspend fun <EVENT : Any, ERROR : Any> Either<JournalError<ERROR>, UpdatedStream<EVENT>>.append(): Either<JournalError<ERROR>, LoadedStream<EVENT>> =
    flatMap { streamToWrite -> this@Journal.append(streamToWrite) }
