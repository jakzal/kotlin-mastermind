package mastermind.game

import arrow.core.*
import arrow.core.raise.either
import mastermind.game.Feedback.Peg.BLACK
import mastermind.game.Feedback.Peg.WHITE
import mastermind.game.GameCommand.JoinGame
import mastermind.game.GameCommand.MakeGuess
import mastermind.game.GameError.GameFinishedError.GameAlreadyLost
import mastermind.game.GameError.GameFinishedError.GameAlreadyWon
import mastermind.game.GameError.GuessError.*
import mastermind.game.GameEvent.*
import kotlin.collections.unzip

sealed interface GameCommand {
    val gameId: GameId

    data class JoinGame(
        override val gameId: GameId,
        val secret: Code,
        val totalAttempts: Int,
        val availablePegs: Set<Code.Peg>
    ) : GameCommand

    data class MakeGuess(override val gameId: GameId, val guess: Code) : GameCommand
}

sealed interface GameEvent {
    val gameId: GameId

    data class GameStarted(
        override val gameId: GameId,
        val secret: Code,
        val totalAttempts: Int,
        val availablePegs: Set<Code.Peg>
    ) : GameEvent

    data class GuessMade(override val gameId: GameId, val guess: Guess) : GameEvent

    data class GameWon(override val gameId: GameId) : GameEvent
    data class GameLost(override val gameId: GameId) : GameEvent
}

data class GameId(val value: String)

data class Code(val pegs: List<Peg>) {
    constructor(vararg pegs: Peg) : this(pegs.toList())
    constructor(vararg pegs: String) : this(pegs.map(::Peg))

    data class Peg(val name: String)

    val length: Int get() = pegs.size
}

data class Guess(val code: Code, val feedback: Feedback)

data class Feedback(val pegs: List<Peg>, val outcome: Outcome) {

    enum class Peg {
        BLACK, WHITE;

        fun formattedName(): String = name.lowercase().replaceFirstChar(Char::uppercase)
    }

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
        data class GuessTooShort(val gameId: GameId, val guess: Code, val requiredLength: Int) : GuessError
        data class GuessTooLong(val gameId: GameId, val guess: Code, val requiredLength: Int) : GuessError
        data class InvalidPegInGuess(val gameId: GameId, val guess: Code, val availablePegs: Set<Code.Peg>) : GuessError
    }
}

typealias Game = NonEmptyList<GameEvent>

private val Game.secret: Code?
    get() = filterIsInstance<GameStarted>().firstOrNull()?.secret

private val Game.secretLength: Int
    get() = this.secret?.length ?: 0

private val Game.secretPegs: List<Code.Peg>
    get() = this.secret?.pegs ?: emptyList()

private val Game.attempts: Int
    get() = filterIsInstance<GuessMade>().size

private val Game.totalAttempts: Int
    get() = filterIsInstance<GameStarted>().firstOrNull()?.totalAttempts ?: 0

private val Game.availablePegs: Set<Code.Peg>
    get() = this.filterIsInstance<GameStarted>().firstOrNull()?.availablePegs ?: emptySet()

private fun Game.isWon(): Boolean =
    this.filterIsInstance<GameWon>().isNotEmpty()

private fun Game.isLost(): Boolean =
    this.filterIsInstance<GameLost>().isNotEmpty()

private fun Game.isStarted(): Boolean =
    this.filterIsInstance<GameStarted>().isNotEmpty()

private fun Game.isGuessTooShort(guess: Code): Boolean =
    guess.pegs.size < this.secretLength

private fun Game.isGuessTooLong(guess: Code): Boolean =
    guess.pegs.size > this.secretLength

private fun Game.isGuessValid(guess: Code): Boolean =
    availablePegs.containsAll(guess.pegs)

private fun Game.exactHits(guess: Code): List<Code.Peg> = this.secretPegs
    .zip(guess.pegs)
    .filter { it.first == it.second }
    .unzip()
    .second

private fun Game.colourHits(guess: Code): List<Code.Peg> = this.secretPegs
    .zip(guess.pegs)
    .filter { (secretColour, guessColour) -> secretColour != guessColour }
    .unzip()
    .let { (secret, guess) ->
        guess.fold(secret to emptyList<Code.Peg>()) { (secretPegs, colourHits), guessPeg ->
            secretPegs.remove(guessPeg)?.let { it to colourHits + guessPeg } ?: (secretPegs to colourHits)
        }.second
    }

/**
 * Removes an element from the list and returns the new list, or null if the element wasn't found.
 */
private fun <T> List<T>.remove(item: T): List<T>? = indexOf(item).let { index ->
    if (index != -1) filterIndexed { i, _ -> i != index }
    else null
}

fun execute(command: GameCommand, game: Game? = null): Either<GameError, NonEmptyList<GameEvent>> = when (command) {
    is JoinGame -> joinGame(command)
    is MakeGuess -> makeGuess(command, game).withOutcome()
}

private fun joinGame(command: JoinGame): Either<Nothing, NonEmptyList<GameStarted>> =
    nonEmptyListOf(GameStarted(command.gameId, command.secret, command.totalAttempts, command.availablePegs)).right()

private fun makeGuess(command: MakeGuess, game: Game?): Either<GameError, GuessMade> = either {
    return startedNotFinishedGame(command, game).flatMap { game ->
        validGuess(command, game).map { guess ->
            GuessMade(command.gameId, Guess(command.guess, game.feedbackOn(guess)))
        }
    }
}

private fun startedNotFinishedGame(command: MakeGuess, game: Game?): Either<GameError, Game> {
    if (game == null || !game.isStarted()) {
        return GameNotStarted(command.gameId).left()
    }
    if (game.isWon()) {
        return GameAlreadyWon(command.gameId).left()
    }
    if (game.isLost()) {
        return GameAlreadyLost(command.gameId).left()
    }
    return game.right()
}

private fun validGuess(command: MakeGuess, game: Game): Either<GameError, Code> {
    if(game.isGuessTooShort(command.guess)) {
        return GuessTooShort(command.gameId, command.guess, game.secretLength).left()
    }
    if(game.isGuessTooLong(command.guess)) {
        return GuessTooLong(command.gameId, command.guess, game.secretLength).left()
    }
    if (!game.isGuessValid(command.guess)) {
        return InvalidPegInGuess(command.gameId, command.guess, game.availablePegs).left()
    }
    return command.guess.right()
}

private fun Either<GameError, GuessMade>.withOutcome(): Either<GameError, NonEmptyList<GameEvent>> = map { event ->
    nonEmptyListOf<GameEvent>(event) +
            when (event.guess.feedback.outcome) {
                Feedback.Outcome.WON -> listOf(GameWon(event.gameId))
                Feedback.Outcome.LOST -> listOf(GameLost(event.gameId))
                else -> emptyList()
            }
}

private fun Game.feedbackOn(guess: Code): Feedback =
    (exactHits(guess).map { BLACK } to colourHits(guess).map { WHITE })
        .let { (exactHits, colourHits) ->
            Feedback(
                exactHits + colourHits,
                when {
                    exactHits.size == this.secretLength -> Feedback.Outcome.WON
                    this.attempts + 1 == this.totalAttempts -> Feedback.Outcome.LOST
                    else -> Feedback.Outcome.IN_PROGRESS
                }
            )
        }

fun setOfPegs(vararg pegs: String): Set<Code.Peg> = pegs.map(Code::Peg).toSet()
