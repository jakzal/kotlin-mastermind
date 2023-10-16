package mastermind.eventsourcing

import arrow.core.NonEmptyList
import arrow.core.toNonEmptyListOrNull

fun <COMMAND : Any, EVENT : Any, ERROR : Any> handlerFor(
    execute: Execute<COMMAND, NonEmptyList<EVENT>?, ERROR, EVENT>
): Execute<COMMAND, List<EVENT>, ERROR, EVENT> =
    { command, events -> execute(command, events.toNonEmptyListOrNull()) }

fun <COMMAND : Any, EVENT : Any, ERROR : Any, STATE> handlerFor(
    execute: Execute<COMMAND, STATE, ERROR, EVENT>,
    applyEvent: Apply<STATE, EVENT>,
    initialState: GetInitialState<STATE>,
): Execute<COMMAND, List<EVENT>, ERROR, EVENT> =
    { command, events -> execute(command, events.fold(initialState(), applyEvent)) }
