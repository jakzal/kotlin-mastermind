package mastermind.game.journal

import arrow.core.Either
import arrow.core.NonEmptyList
import arrow.core.raise.either
import arrow.core.raise.withError
import mastermind.game.journal.Stream.LoadedStream

class InMemoryJournal<EVENT : Any> : Journal<EVENT> {
    private val events = mutableMapOf<String, NonEmptyList<EVENT>>()

    override suspend fun <FAILURE : Any> create(
        streamName: StreamName,
        execute: () -> Either<FAILURE, NonEmptyList<EVENT>>
    ): Either<JournalFailure<FAILURE>, NonEmptyList<EVENT>> = either {
        withError(::ExecutionFailure) {
            execute().onRight { newEvents -> events[streamName] = newEvents }.bind()
        }
    }

    override suspend fun load(streamName: StreamName): Either<EventStoreFailure, LoadedStream<EVENT>> = either {
        events[streamName]?.let {
            LoadedStream(streamName, it.size.toLong(), it)
        } ?: raise(StreamNotFound(streamName))
    }
}