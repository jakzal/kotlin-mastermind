package mastermind.game.journal

import arrow.core.Either
import arrow.core.NonEmptyList
import arrow.core.nonEmptyListOf
import mastermind.game.CommandHandler
import mastermind.journal.Journal
import mastermind.journal.JournalFailure
import mastermind.journal.append

typealias Execute<COMMAND, STATE, FAILURE, EVENT> = (COMMAND, STATE?) -> Either<FAILURE, NonEmptyList<EVENT>>
typealias Apply<STATE, EVENT> = (STATE?, EVENT) -> STATE

context(Journal<EVENT, FAILURE>)
class JournalCommandHandler<COMMAND : Any, EVENT : Any, FAILURE : Any, STATE : Any, RESULT>(
    private val applyEvent: Apply<STATE, EVENT>,
    private val execute: Execute<COMMAND, STATE, FAILURE, EVENT>,
    private val streamNameResolver: (COMMAND) -> String,
    private val calculateResult: (NonEmptyList<EVENT>) -> RESULT
) : CommandHandler<COMMAND, JournalFailure<FAILURE>, RESULT> {

    companion object {
        /**
         * Provides a way to create the command handler with the list of events acting as state
         * without having to provide the apply function for state reconstitution.
         */
        context(Journal<EVENT, FAILURE>)
        operator fun <COMMAND : Any, EVENT : Any, FAILURE : Any, RESULT> invoke(
            execute: Execute<COMMAND, NonEmptyList<EVENT>, FAILURE, EVENT>,
            streamNameResolver: (COMMAND) -> String,
            calculateResult: (NonEmptyList<EVENT>) -> RESULT
        ) = JournalCommandHandler(
            { state: NonEmptyList<EVENT>?, event: EVENT -> state?.let { state + event } ?: nonEmptyListOf(event) },
            execute,
            streamNameResolver,
            calculateResult
        )
    }

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