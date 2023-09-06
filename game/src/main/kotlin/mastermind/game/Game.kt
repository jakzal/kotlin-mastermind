package mastermind.game

import arrow.core.*
import arrow.core.raise.either
import arrow.core.raise.ensure
import mastermind.game.GameCommand.JoinGame
import mastermind.game.GameCommand.MakeGuess
import mastermind.game.GameError.GameFinishedError.GameAlreadyLost
import mastermind.game.GameError.GameFinishedError.GameAlreadyWon
import mastermind.game.GameError.GuessError.GameNotStarted
import mastermind.game.GameError.GuessError.GuessTooShort
import mastermind.game.GameEvent.*
import kotlin.collections.unzip
import kotlin.math.min

sealed interface GameCommand {
    val gameId: GameId

    data class JoinGame(override val gameId: GameId, val secret: Code, val totalAttempts: Int) : GameCommand
    data class MakeGuess(override val gameId: GameId, val guess: Code) : GameCommand
}

sealed interface GameEvent {
    val gameId: GameId

    data class GameStarted(override val gameId: GameId, val secret: Code, val totalAttempts: Int) : GameEvent

    data class GuessMade(override val gameId: GameId, val guess: Guess) : GameEvent

    data class GameWon(override val gameId: GameId) : GameEvent
    data class GameLost(override val gameId: GameId) : GameEvent
}

data class GameId(val value: String)

data class Code(val pegs: List<String>) {
    constructor(vararg pegs: String) : this(pegs.toList())

    val size: Int get() = pegs.size
}

data class Guess(val code: Code, val feedback: Feedback)

data class Feedback(val pegs: List<String>, val outcome: Outcome) {
    enum class Outcome {
        IN_PROGRESS, WON, LOST
    }
}

sealed interface GameError {
    sealed interface GameFinishedError : GameError {
        data class GameAlreadyWon(val gameId: GameId) : GameFinishedError
        data class GameAlreadyLost(val gameId: GameId) : GameFinishedError
    }

    sealed interface GuessError : GameError {
        data class GameNotStarted(val gameId: GameId) : GuessError
        data class GuessTooShort(val gameId: GameId, val guess: Code, val requiredSize: Int): GuessError
        data class GuessTooLong(val gameId: GameId, val guess: Code, val requiredSize: Int) : GuessError
    }
}

typealias Game = NonEmptyList<GameEvent>

private val Game.secret: Code?
    get() = filterIsInstance<GameStarted>().firstOrNull()?.secret

private val Game.attempts: Int
    get() = filterIsInstance<GuessMade>().size

private val Game.totalAttempts: Int
    get() = filterIsInstance<GameStarted>().firstOrNull()?.totalAttempts ?: 0

private fun Game?.isWon(): Boolean =
    this?.filterIsInstance<GameWon>()?.isNotEmpty() ?: false

private fun Game?.isLost(): Boolean =
    this?.filterIsInstance<GameLost>()?.isNotEmpty() ?: false

private fun Game?.isStarted(): Boolean =
    this?.filterIsInstance<GameStarted>()?.isNotEmpty() ?: false

private fun Game?.isGuessToShort(guess: Code): Boolean =
    guess.pegs.size < (this?.secret?.size ?: 0)

private fun Game?.exactHits(guess: Code): List<String> = (this?.secret?.pegs ?: emptyList())
    .zip(guess.pegs)
    .filter { it.first == it.second }
    .unzip()
    .second

private fun Game?.colourHits(guess: Code): List<String> = (this?.secret?.pegs ?: emptyList())
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

fun execute(command: GameCommand, game: Game? = null): Either<GameError, NonEmptyList<GameEvent>> = when (command) {
    is JoinGame -> joinGame(command)
    is MakeGuess -> makeGuess(command, game).withOutcome()
}

private fun joinGame(command: JoinGame): Either<Nothing, NonEmptyList<GameStarted>> =
    nonEmptyListOf(GameStarted(command.gameId, command.secret, command.totalAttempts)).right()

private fun makeGuess(command: MakeGuess, game: Game?): Either<GameError, GuessMade> = either {
    ensure(game.isStarted()) {
        GameNotStarted(command.gameId)
    }
    ensure(!game.isWon()) {
        GameAlreadyWon(command.gameId)
    }
    ensure(!game.isLost()) {
        GameAlreadyLost(command.gameId)
    }
    ensure(!game.isGuessToShort(command.guess)) {
        GuessTooShort(command.gameId, command.guess, game?.secret?.size ?: 0)
    }
    GuessMade(command.gameId, Guess(command.guess, game.feedbackOn(command)))
}

private fun Either<GameError, GuessMade>.withOutcome(): Either<GameError, NonEmptyList<GameEvent>> = map { event ->
    nonEmptyListOf<GameEvent>(event) +
            when (event.guess.feedback.outcome) {
                Feedback.Outcome.WON -> listOf(GameWon(event.gameId))
                Feedback.Outcome.LOST -> listOf(GameLost(event.gameId))
                else -> emptyList()
            }
}

private fun Game?.feedbackOn(command: MakeGuess): Feedback =
    (exactHits(command.guess).map { "Black" } to colourHits(command.guess).map { "White" })
        .let { (exactHits, colourHits) ->
            Feedback(
                exactHits + colourHits,
                when {
                    exactHits.size == this?.secret?.size -> Feedback.Outcome.WON
                    (this?.attempts ?: 0) + 1 == this?.totalAttempts -> Feedback.Outcome.LOST
                    else -> Feedback.Outcome.IN_PROGRESS
                }
            )
        }
