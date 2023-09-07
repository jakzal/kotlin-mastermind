package mastermind.eventsourcing.journal

import arrow.core.Either
import arrow.core.NonEmptyList
import arrow.core.nonEmptyListOf
import mastermind.eventsourcing.Apply
import mastermind.eventsourcing.CommandHandler
import mastermind.eventsourcing.Execute
import mastermind.journal.Journal
import mastermind.journal.JournalFailure
import mastermind.journal.append

context(Journal<EVENT, FAILURE>)
class JournalCommandHandler<COMMAND : Any, EVENT : Any, FAILURE : Any, STATE : Any, OUTCOME>(
    private val applyEvent: Apply<STATE, EVENT>,
    private val execute: Execute<COMMAND, STATE, FAILURE, EVENT>,
    private val streamNameFor: (COMMAND) -> String,
    private val produceOutcome: (NonEmptyList<EVENT>) -> OUTCOME
) : CommandHandler<COMMAND, JournalFailure<FAILURE>, OUTCOME> {

    companion object {
        /**
         * Provides a way to create the command handler with the list of events acting as state
         * without having to provide the apply function for state reconstitution.
         */
        context(Journal<EVENT, FAILURE>)
        operator fun <COMMAND : Any, EVENT : Any, FAILURE : Any, OUTCOME> invoke(
            execute: Execute<COMMAND, NonEmptyList<EVENT>, FAILURE, EVENT>,
            streamNameResolver: (COMMAND) -> String,
            produceOutcome: (NonEmptyList<EVENT>) -> OUTCOME
        ) = JournalCommandHandler(
            { state: NonEmptyList<EVENT>?, event: EVENT -> state?.let { state + event } ?: nonEmptyListOf(event) },
            execute,
            streamNameResolver,
            produceOutcome
        )
    }

    override suspend operator fun invoke(command: COMMAND): Either<JournalFailure<FAILURE>, OUTCOME> {
        return stream(streamNameFor(command)) {
            append {
                execute(command, events.fold(null, applyEvent))
            }
        }.map {
            produceOutcome(it.events)
        }
    }
}