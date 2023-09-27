package mastermind.eventsourcing.journal

import arrow.core.Either
import arrow.core.NonEmptyList
import arrow.core.nonEmptyListOf
import mastermind.eventsourcing.Apply
import mastermind.eventsourcing.CommandHandler
import mastermind.eventsourcing.Execute
import mastermind.journal.JournalError
import mastermind.journal.UpdateStream
import mastermind.journal.append

context(UpdateStream<EVENT, ERROR>)
class JournalCommandHandler<COMMAND : Any, EVENT : Any, ERROR : Any, STATE : Any, OUTCOME>(
    private val applyEvent: Apply<STATE, EVENT>,
    private val execute: Execute<COMMAND, STATE, ERROR, EVENT>,
    private val streamNameFor: (COMMAND) -> String,
    private val produceOutcome: (NonEmptyList<EVENT>) -> OUTCOME
) : CommandHandler<COMMAND, JournalError<ERROR>, OUTCOME> {

    companion object {
        /**
         * Provides a way to create the command handler with the list of events acting as state
         * without having to provide the apply function for state reconstitution.
         */
        context(UpdateStream<EVENT, ERROR>)
        operator fun <COMMAND : Any, EVENT : Any, ERROR : Any, OUTCOME> invoke(
            execute: Execute<COMMAND, NonEmptyList<EVENT>, ERROR, EVENT>,
            streamNameResolver: (COMMAND) -> String,
            produceOutcome: (NonEmptyList<EVENT>) -> OUTCOME
        ) = JournalCommandHandler(
            { state: NonEmptyList<EVENT>?, event: EVENT -> state?.let { state + event } ?: nonEmptyListOf(event) },
            execute,
            streamNameResolver,
            produceOutcome
        )
    }

    override suspend operator fun invoke(command: COMMAND): Either<JournalError<ERROR>, OUTCOME> {
        return this@UpdateStream(streamNameFor(command)) {
            append {
                execute(command, events.fold(null, applyEvent))
            }
        }.map {
            produceOutcome(it.events)
        }
    }
}