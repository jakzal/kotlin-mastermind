package mastermind.game

import arrow.core.Either
import arrow.core.NonEmptyList
import arrow.core.nonEmptyListOf
import arrow.core.raise.either

sealed interface GameCommand {
    val gameId: GameId
}
data class JoinGame(override val gameId: GameId, val secret: Code, val totalAttempts: Int) : GameCommand
data class MakeGuess(override val gameId: GameId, val guess: Code) : GameCommand

sealed interface GameEvent {
    val gameId: GameId
}
data class GameStarted(override val gameId: GameId, val secret: Code, val totalAttempts: Int) : GameEvent

data class GuessMade(override val gameId: GameId, val guess: Guess) : GameEvent

data class Guess(val code: Code, val feedback: Feedback)

data class Feedback(val pegs: List<String>, val outcome: Outcome) {
    enum class Outcome {
        IN_PROGRESS, WON, LOST
    }
}

sealed interface GameFailure

typealias Game = NonEmptyList<GameEvent>

fun execute(command: GameCommand, game: Game? = null): Either<GameFailure, NonEmptyList<GameEvent>> = either {
    when (command) {
        is JoinGame -> nonEmptyListOf(GameStarted(command.gameId, command.secret, command.totalAttempts))
        is MakeGuess -> nonEmptyListOf(GuessMade(command.gameId, Guess(command.guess, Feedback(emptyList(), Feedback.Outcome.IN_PROGRESS))))
    }
}
