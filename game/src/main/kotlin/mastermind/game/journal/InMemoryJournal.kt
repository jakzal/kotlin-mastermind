package mastermind.game.journal

import arrow.core.Either
import arrow.core.NonEmptyList
import arrow.core.raise.either
import arrow.core.raise.withError
import mastermind.game.journal.Stream.LoadedStream
import mastermind.game.journal.Stream.UpdatedStream

class InMemoryJournal<EVENT : Any> : Journal<EVENT> {
    private val events = mutableMapOf<StreamName, NonEmptyList<EVENT>>()

    override suspend fun <FAILURE : Any> stream(
        streamName: StreamName,
        execute: () -> Either<FAILURE, UpdatedStream<EVENT>>
    ): Either<JournalFailure<FAILURE>, LoadedStream<EVENT>> = either {
        withError(::ExecutionFailure) {
            execute().onRight { stream -> events[streamName] = stream.eventsToAppend }.bind().loadedStream(streamName)
        }
    }

    override suspend fun load(streamName: StreamName): Either<EventStoreFailure, LoadedStream<EVENT>> = either {
        events[streamName]?.loadedStream(streamName) ?: raise(StreamNotFound(streamName))
    }

    private fun NonEmptyList<EVENT>.loadedStream(streamName: StreamName) = LoadedStream(streamName, size.toLong(), this)

    private fun UpdatedStream<EVENT>.loadedStream(streamName: StreamName) =
        LoadedStream(streamName, streamVersion + eventsToAppend.size, this.eventsToAppend)
}
