package mastermind.game.journal

import arrow.core.*
import mastermind.journal.JournalFailure
import mastermind.journal.JournalFailure.EventStoreFailure
import mastermind.journal.Stream
import mastermind.journal.Stream.LoadedStream
import mastermind.journal.Stream.UpdatedStream
import mastermind.journal.StreamName

interface Journal<EVENT : Any> {
    suspend fun <FAILURE : Any> stream(
        streamName: StreamName,
        execute: Stream<EVENT>.() -> Either<FAILURE, UpdatedStream<EVENT>>
    ): Either<JournalFailure<FAILURE>, LoadedStream<EVENT>>

    suspend fun load(streamName: StreamName): Either<EventStoreFailure, LoadedStream<EVENT>>
}
