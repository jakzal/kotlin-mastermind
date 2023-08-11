package mastermind.game.journal

import arrow.core.Either
import arrow.core.NonEmptyList

class JournalCommandHandler<COMMAND : Any, EVENT : Any, FAILURE : Any>(
    private val execute: Execute<COMMAND, EVENT, FAILURE>,
    private val streamNameResolver: (COMMAND) -> String
) {
    context(Journal<EVENT>)
    suspend operator fun invoke(command: COMMAND): Either<JournalFailure<FAILURE>, NonEmptyList<EVENT>> {
        return create(streamNameResolver(command)) {
            execute(command)
        }
    }
}