package mastermind.game.journal

import arrow.core.Either
import arrow.core.NonEmptyList

typealias StreamName = String

typealias Execute<COMMAND, EVENT, FAILURE> = (COMMAND) -> Either<FAILURE, NonEmptyList<EVENT>>

interface Journal<EVENT : Any> {
    suspend fun <FAILURE : Any> create(
        streamName: StreamName,
        action: () -> Either<FAILURE, NonEmptyList<EVENT>>
    ): Either<JournalFailure<FAILURE>, NonEmptyList<EVENT>>
}

sealed interface JournalFailure<FAILURE>
data class EventStoreFailure(val cause: Throwable) : JournalFailure<Nothing>
