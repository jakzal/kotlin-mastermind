package mastermind.game

import arrow.core.Either
import mastermind.game.GameCommand.JoinGame
import mastermind.journal.JournalFailure

context(GameIdGenerator, CodeMaker, GameCommandHandler)
suspend fun joinGame(): Either<JournalFailure<GameError>, GameId> =
    this@GameCommandHandler(JoinGame(generateGameId(), makeCode(), 12))

fun interface CodeMaker {
    fun makeCode(): Code
}

fun interface GameIdGenerator {
    fun generateGameId(): GameId
}

typealias GameCommandHandler = CommandHandler<GameCommand, JournalFailure<GameError>, GameId>
