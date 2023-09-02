package mastermind.journal

import arrow.core.Either
import arrow.core.nonEmptyListOf
import arrow.core.raise.either
import arrow.core.raise.withError
import arrow.core.tail
import mastermind.journal.JournalFailure.EventStoreFailure
import mastermind.journal.JournalFailure.EventStoreFailure.StreamNotFound
import mastermind.journal.JournalFailure.ExecutionFailure
import mastermind.journal.Stream.*

class InMemoryJournal<EVENT : Any> : Journal<EVENT> {
    private val events = mutableMapOf<StreamName, LoadedStream<EVENT>>()

    override suspend fun <FAILURE : Any> stream(
        streamName: StreamName,
        execute: Stream<EVENT>.() -> Either<FAILURE, UpdatedStream<EVENT>>
    ): Either<JournalFailure<FAILURE>, LoadedStream<EVENT>> = either {
        withError(::ExecutionFailure) {
            (events[streamName] ?: EmptyStream(streamName))
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

private fun <EVENT : Any> UpdatedStream<EVENT>.toLoadedStream(): LoadedStream<EVENT> {
    val mergedEvents =
        if (this.events.isEmpty()) this.eventsToAppend
        else nonEmptyListOf(this.events.first()) + this.events.tail() + this.eventsToAppend
    return LoadedStream(streamName, this.streamVersion + this.eventsToAppend.size, mergedEvents)
}
