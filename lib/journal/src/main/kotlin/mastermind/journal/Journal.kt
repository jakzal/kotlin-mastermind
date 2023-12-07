package mastermind.journal

import arrow.core.Either
import mastermind.journal.Stream.LoadedStream
import mastermind.journal.Stream.UpdatedStream

interface Journal<EVENT : Any, ERROR : Any> {
    suspend fun load(streamName: StreamName): Either<JournalError<ERROR>, LoadedStream<EVENT>>
    suspend fun append(stream: UpdatedStream<EVENT>): Either<JournalError<ERROR>, LoadedStream<EVENT>>
}