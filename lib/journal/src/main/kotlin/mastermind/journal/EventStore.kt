package mastermind.journal

import arrow.core.Either
import mastermind.journal.JournalFailure.EventStoreFailure
import mastermind.journal.Stream.LoadedStream

interface EventStore<EVENT : Any, FAILURE : Any> {
    fun load(streamName: StreamName): Either<EventStoreFailure<FAILURE>, LoadedStream<EVENT>>
    fun append(stream: Stream.UpdatedStream<EVENT>): Either<EventStoreFailure<FAILURE>, LoadedStream<EVENT>>
}