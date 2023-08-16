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
    ): Either<JournalFailure<FAILURE>, LoadedStream<EVENT>> = either {
        withError(::ExecutionFailure) {
            execute().onRight { newEvents -> events[streamName] = newEvents }.bind().loadedStream(streamName)
        }
    }

    override suspend fun load(streamName: StreamName): Either<EventStoreFailure, LoadedStream<EVENT>> = either {
        events[streamName]?.loadedStream(streamName) ?: raise(StreamNotFound(streamName))
    }

    private fun NonEmptyList<EVENT>.loadedStream(streamName: StreamName) = LoadedStream(streamName, size.toLong(), this)
}
