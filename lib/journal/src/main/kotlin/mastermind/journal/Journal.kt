package mastermind.journal

import arrow.core.Either
import mastermind.journal.JournalFailure.EventStoreFailure
import mastermind.journal.Stream.LoadedStream
import mastermind.journal.Stream.UpdatedStream

interface Journal<EVENT : Any, FAILURE : Any> {
    suspend fun stream(
        streamName: StreamName,
        onStream: Stream<EVENT>.() -> Either<FAILURE, UpdatedStream<EVENT>>
    ): Either<JournalFailure<FAILURE>, LoadedStream<EVENT>>

    suspend fun load(streamName: StreamName): Either<EventStoreFailure<FAILURE>, LoadedStream<EVENT>>
}
