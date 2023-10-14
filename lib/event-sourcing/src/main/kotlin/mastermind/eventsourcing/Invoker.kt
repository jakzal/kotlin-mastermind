package mastermind.eventsourcing

import arrow.core.Either
import arrow.core.NonEmptyList

class Invoker<COMMAND : Any, EVENT : Any, ERROR : Any, STATE>(
    private val applyEvent: Apply<STATE, EVENT>,
    private val execute: Execute<COMMAND, STATE, ERROR, EVENT>,
    private val initialState: GetInitialState<STATE>,
) : Execute<COMMAND, List<EVENT>, ERROR, EVENT> {
    override fun invoke(command: COMMAND, events: List<EVENT>): Either<ERROR, NonEmptyList<EVENT>> {
        return execute(command, events.fold(initialState(), applyEvent))
    }
}