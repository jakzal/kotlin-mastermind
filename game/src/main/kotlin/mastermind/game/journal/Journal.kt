package mastermind.game.journal

import arrow.core.Either
import arrow.core.NonEmptyList
import mastermind.game.journal.Stream.LoadedStream

typealias StreamName = String
typealias StreamVersion = Long

typealias Execute<COMMAND, EVENT, FAILURE> = (COMMAND) -> Either<FAILURE, NonEmptyList<EVENT>>

sealed interface Stream<EVENT : Any> {
    data class LoadedStream<EVENT : Any>(
        val streamName: StreamName,
        val streamVersion: StreamVersion,
        val events: NonEmptyList<EVENT>
    ) : Stream<EVENT>
}

interface Journal<EVENT : Any> {
    suspend fun <FAILURE : Any> stream(
        streamName: StreamName,
        execute: () -> Either<FAILURE, NonEmptyList<EVENT>>
    ): Either<JournalFailure<FAILURE>, LoadedStream<EVENT>>

    suspend fun load(streamName: StreamName): Either<EventStoreFailure, LoadedStream<EVENT>>
}

sealed interface JournalFailure<FAILURE>
sealed interface EventStoreFailure : JournalFailure<Nothing>
data class StreamNotFound(val streamName: StreamName) : EventStoreFailure
data class ExecutionFailure<FAILURE>(val cause: FAILURE) : JournalFailure<FAILURE>
