package mastermind.game.journal

import arrow.core.Either
import arrow.core.NonEmptyList
import mastermind.game.journal.Stream.LoadedStream
import mastermind.game.journal.Stream.UpdatedStream

typealias StreamName = String
typealias StreamVersion = Long

typealias Execute<COMMAND, EVENT, FAILURE> = (COMMAND) -> Either<FAILURE, NonEmptyList<EVENT>>

sealed interface Stream<EVENT : Any> {
    val streamName: StreamName
    val streamVersion: StreamVersion
    val events: List<EVENT>

    data class EmptyStream<EVENT : Any>(override val streamName: StreamName) : Stream<EVENT> {
        override val streamVersion: StreamVersion
            get() = 0
        override val events: List<EVENT>
            get() = emptyList()
    }

    data class LoadedStream<EVENT : Any>(
        override val streamName: StreamName,
        override val streamVersion: StreamVersion,
        override val events: NonEmptyList<EVENT>
    ) : Stream<EVENT>

    data class UpdatedStream<EVENT : Any>(
        override val streamName: StreamName,
        override val streamVersion: StreamVersion,
        override val events: List<EVENT>,
        val eventsToAppend: NonEmptyList<EVENT>
    ) : Stream<EVENT>
}

interface Journal<EVENT : Any> {
    suspend fun <FAILURE : Any> stream(
        streamName: StreamName,
        execute: Stream<EVENT>.() -> Either<FAILURE, UpdatedStream<EVENT>>
    ): Either<JournalFailure<FAILURE>, LoadedStream<EVENT>>

    suspend fun load(streamName: StreamName): Either<EventStoreFailure, LoadedStream<EVENT>>
}

sealed interface JournalFailure<FAILURE>
sealed interface EventStoreFailure : JournalFailure<Nothing>
data class StreamNotFound(val streamName: StreamName) : EventStoreFailure
data class ExecutionFailure<FAILURE>(val cause: FAILURE) : JournalFailure<FAILURE>
