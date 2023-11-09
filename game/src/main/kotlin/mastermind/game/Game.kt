package mastermind.game

import arrow.core.*
import com.fasterxml.jackson.annotation.JsonIgnore
import mastermind.game.Feedback.Outcome.*
import mastermind.game.Feedback.Peg.BLACK
import mastermind.game.Feedback.Peg.WHITE
import mastermind.game.Game.NotStartedGame
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

@JvmInline
value class GameId(val value: String)

data class Code(val pegs: List<Peg>) {
    constructor(vararg pegs: Peg) : this(pegs.toList())
    constructor(vararg pegs: String) : this(pegs.map(::Peg))

    data class Peg(val name: String)

    @get:JsonIgnore
    val length: Int get() = pegs.size
}

data class Guess(val code: Code, val feedback: Feedback)

data class Feedback(val outcome: Outcome, val pegs: List<Peg>) {
    constructor(outcome: Outcome, vararg pegs: Peg) : this(outcome, pegs.toList())

    enum class Peg {
        BLACK, WHITE;

        fun formattedName(): String = name.lowercase().replaceFirstChar(Char::uppercase)
    }

    enum class Outcome {
        IN_PROGRESS, WON, LOST
    }
}

sealed interface GameError {
    val gameId: GameId

    sealed interface GameFinishedError : GameError {
        data class GameAlreadyWon(override val gameId: GameId) : GameFinishedError
        data class GameAlreadyLost(override val gameId: GameId) : GameFinishedError
    }

    sealed interface GuessError : GameError {
        data class GameNotStarted(override val gameId: GameId) : GuessError
        data class GuessTooShort(override val gameId: GameId, val guess: Code, val requiredLength: Int) : GuessError
        data class GuessTooLong(override val gameId: GameId, val guess: Code, val requiredLength: Int) : GuessError
        data class InvalidPegInGuess(override val gameId: GameId, val guess: Code, val availablePegs: Set<Code.Peg>) :
            GuessError
    }
}

sealed interface Game {
    fun applyEvent(event: GameEvent): Game
    fun execute(command: GameCommand): Either<GameError, NonEmptyList<GameEvent>>

    data object NotStartedGame : Game {
        override fun applyEvent(event: GameEvent): Game = when (event) {
            is GameStarted -> StartedGame(event.secret, 0, event.totalAttempts, event.availablePegs)
            else -> this
        }

        override fun execute(command: GameCommand): Either<GameError, NonEmptyList<GameEvent>> =
            when (command) {
                is JoinGame -> nonEmptyListOf(
                    GameStarted(command.gameId, command.secret, command.totalAttempts, command.availablePegs)
                ).right()

                else -> GameNotStarted(command.gameId).left()
            }
    }

    data class StartedGame(
        val secret: Code,
        val attempts: Int,
        val totalAttempts: Int,
        val availablePegs: Set<Code.Peg>
    ) : Game {
        private val secretLength: Int = secret.length

        private val secretPegs: List<Code.Peg> = secret.pegs

        override fun applyEvent(event: GameEvent): Game = when (event) {
            is GameStarted -> this
            is GuessMade -> this.copy(attempts = this.attempts + 1)
            is GameWon -> WonGame
            is GameLost -> LostGame
        }

        override fun execute(command: GameCommand): Either<GameError, NonEmptyList<GameEvent>> = when (command) {
            is MakeGuess -> validGuess(command).map { guess ->
                GuessMade(command.gameId, Guess(command.guess, feedbackOn(guess)))
            }.withOutcome()

            else -> TODO()
        }

        private fun validGuess(command: MakeGuess): Either<GameError, Code> {
            if (isGuessTooShort(command.guess)) {
                return GuessTooShort(command.gameId, command.guess, secretLength).left()
            }
            if (isGuessTooLong(command.guess)) {
                return GuessTooLong(command.gameId, command.guess, secretLength).left()
            }
            if (!isGuessValid(command.guess)) {
                return InvalidPegInGuess(command.gameId, command.guess, availablePegs).left()
            }
            return command.guess.right()
        }

        private fun isGuessTooShort(guess: Code): Boolean =
            guess.length < secretLength

        private fun isGuessTooLong(guess: Code): Boolean =
            guess.length > secretLength

        private fun isGuessValid(guess: Code): Boolean =
            availablePegs.containsAll(guess.pegs)

        private fun feedbackOn(guess: Code): Feedback =
            feedbackPegsOn(guess)
                .let { (exactHits, colourHits) ->
                    Feedback(outcomeFor(exactHits), exactHits + colourHits)
                }

        private fun feedbackPegsOn(guess: Code) =
            exactHits(guess).map { BLACK } to colourHits(guess).map { WHITE }

        private fun outcomeFor(exactHits: List<Feedback.Peg>) = when {
            exactHits.size == this.secretLength -> WON
            this.attempts + 1 == this.totalAttempts -> LOST
            else -> IN_PROGRESS
        }

        private fun exactHits(guess: Code): List<Code.Peg> = this.secretPegs
            .zip(guess.pegs)
            .filter { (secretColour, guessColour) -> secretColour == guessColour }
            .unzip()
            .second

        private fun colourHits(guess: Code): List<Code.Peg> = this.secretPegs
            .zip(guess.pegs)
            .filter { (secretColour, guessColour) -> secretColour != guessColour }
            .unzip()
            .let { (secret, guess) ->
                guess.fold(secret to emptyList<Code.Peg>()) { (secretPegs, colourHits), guessPeg ->
                    secretPegs.remove(guessPeg)?.let { it to colourHits + guessPeg } ?: (secretPegs to colourHits)
                }.second
            }

        private fun Either<GameError, GuessMade>.withOutcome(): Either<GameError, NonEmptyList<GameEvent>> =
            map { event ->
                nonEmptyListOf<GameEvent>(event) +
                        when (event.guess.feedback.outcome) {
                            WON -> listOf(GameWon(event.gameId))
                            LOST -> listOf(GameLost(event.gameId))
                            else -> emptyList()
                        }
            }
    }

    data object WonGame : Game {
        override fun applyEvent(event: GameEvent): Game = this
        override fun execute(command: GameCommand): Either<GameError, NonEmptyList<GameEvent>> =
            GameAlreadyWon(command.gameId).left()
    }

    data object LostGame : Game {
        override fun applyEvent(event: GameEvent): Game = this
        override fun execute(command: GameCommand): Either<GameError, NonEmptyList<GameEvent>> =
            GameAlreadyLost(command.gameId).left()
    }
}

fun execute(
    command: GameCommand,
    game: Game = notStartedGame()
): Either<GameError, NonEmptyList<GameEvent>> =
    game.execute(command)

/**
 * Removes an element from the list and returns the new list, or null if the element wasn't found.
 */
private fun <T> List<T>.remove(item: T): List<T>? = indexOf(item).let { index ->
    if (index != -1) filterIndexed { i, _ -> i != index }
    else null
}

fun notStartedGame(): Game = NotStartedGame

fun setOfPegs(vararg pegs: String): Set<Code.Peg> = pegs.map(Code::Peg).toSet()
