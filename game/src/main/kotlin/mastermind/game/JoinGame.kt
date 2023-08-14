package mastermind.game

import arrow.core.Either
import mastermind.game.journal.JournalFailure

context(GameIdGenerator, CodeMaker, GameCommandHandler)
suspend fun joinGame(): Either<JournalFailure<GameFailure>, GameId> =
    this@GameCommandHandler(JoinGame(generateGameId(), makeCode(), 12))

fun interface CodeMaker {
    fun makeCode(): Code
}

fun interface GameIdGenerator {
    fun generateGameId(): GameId
}

typealias GameCommandHandler = CommandHandler<GameCommand, JournalFailure<GameFailure>, GameId>
