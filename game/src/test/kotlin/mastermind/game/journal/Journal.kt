package mastermind.game.journal

import arrow.core.Either
import arrow.core.NonEmptyList

typealias StreamName = String

typealias Execute<COMMAND, EVENT, FAILURE> = (COMMAND) -> Either<FAILURE, NonEmptyList<EVENT>>

interface Journal<EVENT : Any> {
    suspend fun <FAILURE : Any> create(
        streamName: StreamName,
        execute: () -> Either<FAILURE, NonEmptyList<EVENT>>
    ): Either<JournalFailure<FAILURE>, NonEmptyList<EVENT>>

    suspend fun load(streamName: StreamName): Either<EventStoreFailure, NonEmptyList<EVENT>>
}

sealed interface JournalFailure<FAILURE>
sealed interface EventStoreFailure : JournalFailure<Nothing>
data class StreamNotFound(val streamName: StreamName) : EventStoreFailure
data class ExecutionFailure<FAILURE>(val cause: FAILURE) : JournalFailure<FAILURE>
