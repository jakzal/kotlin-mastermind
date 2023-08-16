package mastermind.game.journal

import arrow.core.Either
import arrow.core.NonEmptyList
import mastermind.game.CommandHandler

context(Journal<EVENT>)
class JournalCommandHandler<COMMAND : Any, EVENT : Any, FAILURE : Any, RESULT>(
    private val execute: Execute<COMMAND, EVENT, FAILURE>,
    private val streamNameResolver: (COMMAND) -> String,
    private val calculateResult: (NonEmptyList<EVENT>) -> RESULT
) : CommandHandler<COMMAND, JournalFailure<FAILURE>, RESULT> {
    override suspend operator fun invoke(command: COMMAND): Either<JournalFailure<FAILURE>, RESULT> {
        return stream(streamNameResolver(command)) {
            append {
                execute(command)
            }
        }.map {
            calculateResult(it.events)
        }
    }
}