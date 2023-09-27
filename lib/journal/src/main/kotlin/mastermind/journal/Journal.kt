package mastermind.journal

import arrow.core.Either
import mastermind.journal.Stream.LoadedStream
import mastermind.journal.Stream.UpdatedStream

interface Journal<EVENT : Any, FAILURE : Any> {
    suspend fun load(streamName: StreamName): Either<JournalFailure<FAILURE>, LoadedStream<EVENT>>
    suspend fun append(stream: UpdatedStream<EVENT>): Either<JournalFailure<FAILURE>, LoadedStream<EVENT>>
}