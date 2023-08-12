package mastermind.game.journal

import arrow.core.Either
import arrow.core.NonEmptyList

class JournalCommandHandler<COMMAND : Any, EVENT : Any, FAILURE : Any, RESULT>(
    private val execute: Execute<COMMAND, EVENT, FAILURE>,
    private val streamNameResolver: (COMMAND) -> String,
    private val calculateResult: (NonEmptyList<EVENT>) -> RESULT
) {
    context(Journal<EVENT>)
    suspend operator fun invoke(command: COMMAND): Either<JournalFailure<FAILURE>, RESULT> {
        return create(streamNameResolver(command)) {
            execute(command)
        }.map(calculateResult)
    }
}