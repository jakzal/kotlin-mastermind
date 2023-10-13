package mastermind.eventsourcing.journal

import arrow.core.Either
import arrow.core.NonEmptyList
import arrow.core.nonEmptyListOf
import mastermind.eventsourcing.Apply
import mastermind.eventsourcing.CommandHandler
import mastermind.eventsourcing.Execute
import mastermind.eventsourcing.GetInitialState
import mastermind.journal.JournalError
import mastermind.journal.JournalError.ExecutionError
import mastermind.journal.UpdateStream
import mastermind.journal.append

context(UpdateStream<EVENT, JournalError<ERROR>>)
class JournalCommandHandler<COMMAND : Any, EVENT : Any, ERROR : Any, STATE, OUTCOME>(
    private val applyEvent: Apply<STATE, EVENT>,
    private val execute: Execute<COMMAND, STATE, ERROR, EVENT>,
    private val initialState: GetInitialState<STATE>,
    private val streamNameFor: (COMMAND) -> String,
    private val produceOutcome: (NonEmptyList<EVENT>) -> OUTCOME
) : CommandHandler<COMMAND, JournalError<ERROR>, OUTCOME> {

    companion object {
        /**
         * Provides a way to create the command handler with the list of events acting as state
         * without having to provide the apply function for state reconstitution.
         * The list of events is non-empty, but can be null before the first event is appended to the journal.
         */
        context(UpdateStream<EVENT, JournalError<ERROR>>)
        operator fun <COMMAND : Any, EVENT : Any, ERROR : Any, OUTCOME> invoke(
            execute: Execute<COMMAND, NonEmptyList<EVENT>?, ERROR, EVENT>,
            streamNameResolver: (COMMAND) -> String,
            produceOutcome: (NonEmptyList<EVENT>) -> OUTCOME
        ) = JournalCommandHandler(
            { state: NonEmptyList<EVENT>?, event: EVENT -> state?.let { state + event } ?: nonEmptyListOf(event) },
            execute,
            { null },
            streamNameResolver,
            produceOutcome
        )
    }

    override suspend operator fun invoke(command: COMMAND): Either<JournalError<ERROR>, OUTCOME> {
        return this@UpdateStream(streamNameFor(command)) {
            append {
                execute(command, events.fold(initialState(), applyEvent))
                    .mapLeft(::ExecutionError)
            }
        }.map {
            produceOutcome(it.events)
        }
    }
}