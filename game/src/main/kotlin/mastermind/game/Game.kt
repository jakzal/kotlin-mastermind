package mastermind.game

import arrow.core.Either
import arrow.core.NonEmptyList
import arrow.core.nonEmptyListOf
import arrow.core.raise.either

sealed interface GameCommand {
    val gameId: GameId
}
data class JoinGame(override val gameId: GameId, val secret: Code, val totalAttempts: Int) : GameCommand

sealed interface GameEvent {
    val gameId: GameId
}
data class GameStarted(override val gameId: GameId, val secret: Code, val totalAttempts: Int) : GameEvent

sealed interface GameFailure

fun execute(command: GameCommand): Either<GameFailure, NonEmptyList<GameEvent>> = either {
    when (command) {
        is JoinGame -> nonEmptyListOf(GameStarted(command.gameId, command.secret, command.totalAttempts))
    }
}
