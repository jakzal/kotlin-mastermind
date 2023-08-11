package mastermind.game.journal

import arrow.core.Either
import arrow.core.NonEmptyList

typealias StreamName = String

typealias Execute<COMMAND, EVENT, FAILURE> = (COMMAND) -> Either<FAILURE, NonEmptyList<EVENT>>

interface Journal<EVENT : Any> {
    suspend fun create(
        streamName: StreamName,
        action: () -> NonEmptyList<EVENT>
    ): Either<JournalFailure, NonEmptyList<EVENT>>
}

sealed interface JournalFailure
data class EventStoreFailure(val cause: Throwable) : JournalFailure
