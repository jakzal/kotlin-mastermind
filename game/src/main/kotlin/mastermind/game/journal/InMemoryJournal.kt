package mastermind.game.journal

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.withError
import mastermind.journal.Stream
import mastermind.journal.Stream.LoadedStream
import mastermind.journal.Stream.UpdatedStream
import mastermind.journal.StreamName

class InMemoryJournal<EVENT : Any> : Journal<EVENT> {
    private val events = mutableMapOf<StreamName, LoadedStream<EVENT>>()

    override suspend fun <FAILURE : Any> stream(
        streamName: StreamName,
        execute: Stream<EVENT>.() -> Either<FAILURE, UpdatedStream<EVENT>>
    ): Either<JournalFailure<FAILURE>, LoadedStream<EVENT>> = either {
        withError(::ExecutionFailure) {
            (events[streamName] ?: Stream.EmptyStream(streamName))
                .execute()
                .map { stream -> stream.toLoadedStream() }
                .onRight { stream ->
                    events[streamName] = stream
                }
                .bind()
        }
    }

    override suspend fun load(streamName: StreamName): Either<EventStoreFailure, LoadedStream<EVENT>> = either {
        events[streamName] ?: raise(StreamNotFound(streamName))
    }
}
