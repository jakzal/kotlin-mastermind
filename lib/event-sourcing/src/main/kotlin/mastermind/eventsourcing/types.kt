package mastermind.eventsourcing

import arrow.core.Either
import arrow.core.NonEmptyList

typealias Execute<COMMAND, STATE, FAILURE, EVENT> = (COMMAND, STATE?) -> Either<FAILURE, NonEmptyList<EVENT>>

typealias Apply<STATE, EVENT> = (STATE?, EVENT) -> STATE

typealias CommandHandler<COMMAND, FAILURE, OUTCOME> = suspend (COMMAND) -> Either<FAILURE, OUTCOME>
