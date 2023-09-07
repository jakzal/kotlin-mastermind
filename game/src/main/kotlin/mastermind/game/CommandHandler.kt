package mastermind.game

import arrow.core.Either

typealias CommandHandler<COMMAND, FAILURE, OUTCOME> = suspend (COMMAND) -> Either<FAILURE, OUTCOME>
