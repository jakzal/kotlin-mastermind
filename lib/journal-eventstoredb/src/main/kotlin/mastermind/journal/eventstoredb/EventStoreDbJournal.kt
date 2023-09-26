package mastermind.journal.eventstoredb

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.recover
import mastermind.journal.Journal
import mastermind.journal.JournalFailure
import mastermind.journal.JournalFailure.EventStoreFailure
import mastermind.journal.JournalFailure.EventStoreFailure.StreamNotFound
import mastermind.journal.JournalFailure.ExecutionFailure
import mastermind.journal.Stream
import mastermind.journal.Stream.*
import mastermind.journal.StreamName

class EventStoreDbJournal<EVENT : Any, FAILURE : Any>(
    private val eventStoreDb: EventStoreDb<EVENT, FAILURE>
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
        eventStoreDb.load(streamName)

    private fun Either<EventStoreFailure<FAILURE>, LoadedStream<EVENT>>.orCreate(streamName: StreamName): Either<JournalFailure<FAILURE>, Stream<EVENT>> =
        recover<JournalFailure<FAILURE>, JournalFailure<FAILURE>, Stream<EVENT>> { e ->
            if (e is StreamNotFound) EmptyStream(streamName)
            else raise(e)
        }

    private fun Either<JournalFailure<FAILURE>, Stream<EVENT>>.execute(onStream: Stream<EVENT>.() -> Either<FAILURE, UpdatedStream<EVENT>>): Either<JournalFailure<FAILURE>, UpdatedStream<EVENT>> =
        flatMap { stream -> stream.onStream().mapLeft(::ExecutionFailure) }

    private suspend fun Either<JournalFailure<FAILURE>, UpdatedStream<EVENT>>.append(): Either<JournalFailure<FAILURE>, LoadedStream<EVENT>> =
        flatMap { stream -> eventStoreDb.append(stream) }
}