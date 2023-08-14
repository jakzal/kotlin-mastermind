package mastermind.game

import arrow.core.Either

typealias CommandHandler<COMMAND, FAILURE, RESULT> = suspend (COMMAND) -> Either<FAILURE, RESULT>
