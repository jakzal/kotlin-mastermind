package mastermind.journal

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.recover
import mastermind.journal.JournalFailure.EventStoreFailure
import mastermind.journal.JournalFailure.EventStoreFailure.StreamNotFound
import mastermind.journal.JournalFailure.ExecutionFailure
import mastermind.journal.Stream.*

context(EventStore<EVENT, FAILURE>)
fun <EVENT : Any, FAILURE : Any> createUpdateStream(): UpdateStream<EVENT, FAILURE> = with(this@EventStore) {
    { streamName, onStream ->
        load(streamName)
            .orCreate(streamName)
            .execute(onStream)
            .append()
    }
}

private fun <EVENT : Any, FAILURE : Any> Either<EventStoreFailure<FAILURE>, LoadedStream<EVENT>>.orCreate(streamName: StreamName): Either<JournalFailure<FAILURE>, Stream<EVENT>> =
    recover<JournalFailure<FAILURE>, JournalFailure<FAILURE>, Stream<EVENT>> { e ->
        if (e is StreamNotFound) EmptyStream(streamName)
        else raise(e)
    }

private fun <EVENT : Any, FAILURE : Any> Either<JournalFailure<FAILURE>, Stream<EVENT>>.execute(onStream: Stream<EVENT>.() -> Either<FAILURE, UpdatedStream<EVENT>>): Either<JournalFailure<FAILURE>, UpdatedStream<EVENT>> =
    flatMap { stream -> stream.onStream().mapLeft(::ExecutionFailure) }

context(EventStore<EVENT, FAILURE>)
private suspend fun <EVENT : Any, FAILURE : Any> Either<JournalFailure<FAILURE>, UpdatedStream<EVENT>>.append(): Either<JournalFailure<FAILURE>, LoadedStream<EVENT>> =
    flatMap { streamToWrite -> this@EventStore.append(streamToWrite) }
