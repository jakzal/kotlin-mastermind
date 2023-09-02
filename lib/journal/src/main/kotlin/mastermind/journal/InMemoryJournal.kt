package mastermind.journal

import arrow.core.Either
import arrow.core.NonEmptyList
import arrow.core.getOrElse
import arrow.core.raise.either
import arrow.core.raise.withError
import arrow.core.toNonEmptyListOrNone
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

private fun <EVENT : Any> UpdatedStream<EVENT>.toLoadedStream(): LoadedStream<EVENT> =
    LoadedStream(streamName, streamVersion + eventsToAppend.size, events.append(eventsToAppend))

private fun <EVENT : Any> List<EVENT>.append(eventsToAppend: NonEmptyList<EVENT>): NonEmptyList<EVENT> =
    toNonEmptyListOrNone()
        .map { events -> events + eventsToAppend }
        .getOrElse { eventsToAppend }
