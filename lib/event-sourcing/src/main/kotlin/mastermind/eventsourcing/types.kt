package mastermind.eventsourcing

import arrow.core.Either
import arrow.core.NonEmptyList

typealias Execute<COMMAND, STATE, ERROR, EVENT> = (COMMAND, STATE) -> Either<ERROR, NonEmptyList<EVENT>>

typealias Apply<STATE, EVENT> = (STATE, EVENT) -> STATE

typealias GetInitialState<STATE> = () -> STATE

typealias CommandDispatcher<COMMAND, ERROR, OUTCOME> = suspend (COMMAND) -> Either<ERROR, OUTCOME>
