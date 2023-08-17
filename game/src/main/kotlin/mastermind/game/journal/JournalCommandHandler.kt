package mastermind.game.journal

import arrow.core.Either
import arrow.core.NonEmptyList
import arrow.core.toNonEmptyListOrNull
import mastermind.game.CommandHandler

context(Journal<EVENT>)
class JournalCommandHandler<COMMAND : Any, EVENT : Any, FAILURE : Any, RESULT>(
    private val execute: Execute<COMMAND, EVENT, NonEmptyList<EVENT>, FAILURE>,
    private val streamNameResolver: (COMMAND) -> String,
    private val calculateResult: (NonEmptyList<EVENT>) -> RESULT
) : CommandHandler<COMMAND, JournalFailure<FAILURE>, RESULT> {
    override suspend operator fun invoke(command: COMMAND): Either<JournalFailure<FAILURE>, RESULT> {
        return stream(streamNameResolver(command)) {
            append {
                execute(command, this.events.toNonEmptyListOrNull())
            }
        }.map {
            calculateResult(it.events)
        }
    }
}