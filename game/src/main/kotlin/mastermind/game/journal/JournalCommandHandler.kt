package mastermind.game.journal

import arrow.core.Either
import arrow.core.NonEmptyList
import mastermind.game.CommandHandler
import mastermind.journal.Journal
import mastermind.journal.JournalFailure
import mastermind.journal.append

// @TODO sort out order of type parameters

typealias Execute<COMMAND, EVENT, STATE, FAILURE> = (COMMAND, STATE?) -> Either<FAILURE, NonEmptyList<EVENT>>
typealias Apply<STATE, EVENT> = (STATE?, EVENT) -> STATE

context(Journal<EVENT, FAILURE>)
class JournalCommandHandler<COMMAND : Any, EVENT : Any, FAILURE : Any, RESULT, STATE : Any>(
    private val applyEvent: Apply<STATE, EVENT>,
    private val execute: Execute<COMMAND, EVENT, STATE, FAILURE>,
    private val streamNameResolver: (COMMAND) -> String,
    private val calculateResult: (NonEmptyList<EVENT>) -> RESULT
) : CommandHandler<COMMAND, JournalFailure<FAILURE>, RESULT> {
    override suspend operator fun invoke(command: COMMAND): Either<JournalFailure<FAILURE>, RESULT> {
        return stream(streamNameResolver(command)) {
            append {
                execute(command, events.fold(null, applyEvent))
            }
        }.map {
            calculateResult(it.events)
        }
    }
}