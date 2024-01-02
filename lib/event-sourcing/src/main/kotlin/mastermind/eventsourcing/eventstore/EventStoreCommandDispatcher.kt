package mastermind.eventsourcing.eventstore

import arrow.core.Either
import arrow.core.NonEmptyList
import mastermind.eventsourcing.Dispatch
import mastermind.eventsourcing.Execute
import mastermind.eventstore.EventStore
import mastermind.eventstore.StreamName


class EventStoreCommandDispatcher<COMMAND : Any, EVENT : Any, ERROR : Any, OUTCOME>(
    private val eventStore: EventStore<EVENT>,
    private val execute: Execute<COMMAND, List<EVENT>, ERROR, EVENT>,
    private val streamNameFor: (COMMAND) -> StreamName,
    private val produceOutcome: (NonEmptyList<EVENT>) -> OUTCOME
) : Dispatch<COMMAND, EventSourcingError<ERROR>, OUTCOME> {

    override suspend operator fun invoke(command: COMMAND): Either<EventSourcingError<ERROR>, OUTCOME> =
        with(eventStore) {
            loadToAppend(streamNameFor(command)) { events ->
                execute(command, events)
            }.map(produceOutcome)
        }
}
