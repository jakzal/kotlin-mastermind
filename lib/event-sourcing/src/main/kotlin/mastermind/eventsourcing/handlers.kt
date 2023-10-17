package mastermind.eventsourcing

fun <COMMAND : Any, EVENT : Any, ERROR : Any, STATE> handlerFor(
    execute: Execute<COMMAND, STATE, ERROR, EVENT>,
    applyEvent: Apply<STATE, EVENT>,
    initialState: GetInitialState<STATE>,
): Execute<COMMAND, List<EVENT>, ERROR, EVENT> =
    { command, events -> execute(command, events.fold(initialState(), applyEvent)) }
