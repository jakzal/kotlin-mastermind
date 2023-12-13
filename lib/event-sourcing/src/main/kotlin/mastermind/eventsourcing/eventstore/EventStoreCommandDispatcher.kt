package mastermind.eventsourcing.eventstore

import arrow.core.Either
import arrow.core.NonEmptyList
import mastermind.eventsourcing.Dispatch
import mastermind.eventsourcing.Execute
import mastermind.eventstore.EventSourcingError
import mastermind.eventstore.EventStore
import mastermind.eventstore.StreamName
import mastermind.eventstore.loadToAppend


context(EventStore<EVENT>)
class EventStoreCommandDispatcher<COMMAND : Any, EVENT : Any, ERROR : Any, OUTCOME>(
    private val execute: Execute<COMMAND, List<EVENT>, ERROR, EVENT>,
    private val streamNameFor: (COMMAND) -> StreamName,
    private val produceOutcome: (NonEmptyList<EVENT>) -> OUTCOME
) : Dispatch<COMMAND, EventSourcingError<ERROR>, OUTCOME> {

    override suspend operator fun invoke(command: COMMAND): Either<EventSourcingError<ERROR>, OUTCOME> =
        loadToAppend(streamNameFor(command)) { events ->
            execute(command, events)
        }.map(produceOutcome)
}
