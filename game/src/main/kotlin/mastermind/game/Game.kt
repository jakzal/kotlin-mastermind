package mastermind.game

import arrow.core.*
import arrow.core.raise.either
import kotlin.collections.unzip
import kotlin.math.min

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

sealed interface GameFinishedFailure : GameFailure {
    data class GameWonFailure(val gameId: GameId) : GameFinishedFailure
}

typealias Game = NonEmptyList<GameEvent>

private val NonEmptyList<GameEvent>.secret: Code?
    get() = filterIsInstance<GameStarted>().first().secret

private fun NonEmptyList<GameEvent>?.exactHits(guess: Code): List<String> = (this?.secret?.pegs ?: emptyList())
    .zip(guess.pegs)
    .filter { it.first == it.second }
    .unzip()
    .second

private fun NonEmptyList<GameEvent>?.colourHits(guess: Code): List<String> = (this?.secret?.pegs ?: emptyList())
    .zip(guess.pegs)
    .filter { (secretColour, guessColour) -> secretColour != guessColour }
    .unzip()
    .let { (secret, guess) ->
        val secretGrouped = secret.groupBy { colour -> colour }.mapValues { colours -> colours.value.size }
        val guessGrouped = guess.groupBy { colour -> colour }.mapValues { colours -> colours.value.size }
        secretGrouped
            .zip(guessGrouped)
            .map { (colour, occurrences) -> (1..min(occurrences.first, occurrences.second)).map { colour } }
            .flatten()
    }

fun execute(command: GameCommand, game: Game? = null): Either<GameFailure, NonEmptyList<GameEvent>> = either {
    when (command) {
        is JoinGame -> nonEmptyListOf(GameStarted(command.gameId, command.secret, command.totalAttempts))
        is MakeGuess -> nonEmptyListOf(
            GuessMade(
                command.gameId, Guess(
                    command.guess,
                    Feedback(
                        game.exactHits(command.guess).map { "Black" } + game.colourHits(command.guess).map { "White" },
                        Feedback.Outcome.IN_PROGRESS
                    )
                )
            )
        )
    }
}
