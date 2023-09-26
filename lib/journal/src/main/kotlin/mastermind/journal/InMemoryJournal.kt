package mastermind.journal

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.recover
import mastermind.journal.JournalFailure.EventStoreFailure
import mastermind.journal.JournalFailure.EventStoreFailure.StreamNotFound
import mastermind.journal.JournalFailure.ExecutionFailure
import mastermind.journal.Stream.*


class InMemoryJournal<EVENT : Any, FAILURE : Any>(
    private val eventStore: InMemoryEventStore<EVENT, FAILURE> = InMemoryEventStore()
) : Journal<EVENT, FAILURE> {

    override suspend fun stream(
        streamName: StreamName,
        onStream: Stream<EVENT>.() -> Either<FAILURE, UpdatedStream<EVENT>>
    ): Either<JournalFailure<FAILURE>, LoadedStream<EVENT>> =
        load(streamName)
            .orCreate(streamName)
            .execute(onStream)
            .append()

    override suspend fun load(streamName: StreamName): Either<EventStoreFailure<FAILURE>, LoadedStream<EVENT>> =
        (eventStore::load)(streamName)

    private fun Either<EventStoreFailure<FAILURE>, LoadedStream<EVENT>>.orCreate(streamName: StreamName): Either<JournalFailure<FAILURE>, Stream<EVENT>> =
        recover<JournalFailure<FAILURE>, JournalFailure<FAILURE>, Stream<EVENT>> { e ->
            if (e is StreamNotFound) EmptyStream(streamName)
            else raise(e)
        }

    private fun Either<JournalFailure<FAILURE>, Stream<EVENT>>.execute(onStream: Stream<EVENT>.() -> Either<FAILURE, UpdatedStream<EVENT>>): Either<JournalFailure<FAILURE>, UpdatedStream<EVENT>> =
        flatMap { stream -> stream.onStream().mapLeft(::ExecutionFailure) }

    private suspend fun Either<JournalFailure<FAILURE>, UpdatedStream<EVENT>>.append(): Either<JournalFailure<FAILURE>, LoadedStream<EVENT>> =
        flatMap { streamToWrite -> eventStore.append(streamToWrite) }
}

