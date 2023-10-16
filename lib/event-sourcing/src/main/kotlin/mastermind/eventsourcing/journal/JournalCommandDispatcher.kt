package mastermind.eventsourcing.journal

import arrow.core.Either
import arrow.core.NonEmptyList
import mastermind.eventsourcing.Dispatch
import mastermind.eventsourcing.Execute
import mastermind.journal.JournalError
import mastermind.journal.JournalError.ExecutionError
import mastermind.journal.UpdateStream
import mastermind.journal.append


context(UpdateStream<EVENT, JournalError<ERROR>>)
class JournalCommandDispatcher<COMMAND : Any, EVENT : Any, ERROR : Any, OUTCOME>(
    private val execute: Execute<COMMAND, List<EVENT>, ERROR, EVENT>,
    private val streamNameFor: (COMMAND) -> String,
    private val produceOutcome: (NonEmptyList<EVENT>) -> OUTCOME
) : Dispatch<COMMAND, JournalError<ERROR>, OUTCOME> {

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