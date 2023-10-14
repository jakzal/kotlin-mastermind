package mastermind.eventsourcing.journal

import arrow.core.Either
import arrow.core.NonEmptyList
import arrow.core.toNonEmptyListOrNull
import mastermind.eventsourcing.Apply
import mastermind.eventsourcing.CommandHandler
import mastermind.eventsourcing.Execute
import mastermind.eventsourcing.GetInitialState
import mastermind.journal.JournalError
import mastermind.journal.JournalError.ExecutionError
import mastermind.journal.UpdateStream
import mastermind.journal.append


class Invoker<COMMAND : Any, EVENT : Any, ERROR : Any, STATE>(
    private val applyEvent: Apply<STATE, EVENT>,
    private val execute: Execute<COMMAND, STATE, ERROR, EVENT>,
    private val initialState: GetInitialState<STATE>,
) : Execute<COMMAND, List<EVENT>, ERROR, EVENT> {
    override fun invoke(command: COMMAND, events: List<EVENT>): Either<ERROR, NonEmptyList<EVENT>> {
        return execute(command, events.fold(initialState(), applyEvent))
    }
}

class NoStateInvoker<COMMAND : Any, EVENT : Any, ERROR : Any>(
    private val execute: Execute<COMMAND, NonEmptyList<EVENT>?, ERROR, EVENT>,
) : Execute<COMMAND, List<EVENT>, ERROR, EVENT> {
    override fun invoke(command: COMMAND, events: List<EVENT>): Either<ERROR, NonEmptyList<EVENT>> {
        return execute(command, events.toNonEmptyListOrNull())
    }
}

context(UpdateStream<EVENT, JournalError<ERROR>>)
class JournalCommandHandler<COMMAND : Any, EVENT : Any, ERROR : Any, OUTCOME>(
    private val execute: Execute<COMMAND, List<EVENT>, ERROR, EVENT>,
    private val streamNameFor: (COMMAND) -> String,
    private val produceOutcome: (NonEmptyList<EVENT>) -> OUTCOME
) : CommandHandler<COMMAND, JournalError<ERROR>, OUTCOME> {

    override suspend operator fun invoke(command: COMMAND): Either<JournalError<ERROR>, OUTCOME> {
        return this@UpdateStream(streamNameFor(command)) {
            append {
                execute(command, events).mapLeft(::ExecutionError)
            }
        }.map {
            produceOutcome(it.events)
        }
    }
}