package mastermind.game

import arrow.core.Either
import arrow.core.NonEmptyList
import arrow.core.getOrElse
import mastermind.game.Feedback.Outcome.*
import mastermind.game.Feedback.Peg.BLACK
import mastermind.game.Feedback.Peg.WHITE
import mastermind.game.GameCommand.JoinGame
import mastermind.game.GameCommand.MakeGuess
import mastermind.game.GameError.GameFinishedError.GameAlreadyLost
import mastermind.game.GameError.GameFinishedError.GameAlreadyWon
import mastermind.game.GameError.GuessError.*
import mastermind.game.GameEvent.*
import mastermind.game.testkit.anyGameId
import mastermind.testkit.assertions.shouldFailWith
import mastermind.testkit.assertions.shouldSucceedWith
import mastermind.testkit.dynamictest.dynamicTestsFor
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory

class GameExamples {
    private val gameId = anyGameId()
    private val secret = Code("Red", "Green", "Blue", "Yellow")
    private val totalAttempts = 12
    private val availablePegs = setOfPegs("Red", "Green", "Blue", "Yellow", "Purple", "Pink")

    @Test
    fun `it starts the game`() {
        execute(JoinGame(gameId, secret, totalAttempts, availablePegs)) shouldSucceedWith listOf(
            GameStarted(
                gameId,
                secret,
                totalAttempts,
                availablePegs
            )
        )
    }

    @Test
    fun `it makes a guess`() {
        val game = gameOf(GameStarted(gameId, secret, totalAttempts, availablePegs))

        execute(MakeGuess(gameId, Code("Purple", "Purple", "Purple", "Purple")), game) shouldSucceedWith listOf(
            GuessMade(
                gameId,
                Guess(
                    Code("Purple", "Purple", "Purple", "Purple"),
                    Feedback(IN_PROGRESS)
                )
            )
        )
    }

    @TestFactory
    fun `it gives feedback on the guess`() = guessExamples { (secret: Code, guess: Code, feedback: Feedback) ->
        val game = gameOf(GameStarted(gameId, secret, totalAttempts, availablePegs))

        execute(MakeGuess(gameId, guess), game) shouldSucceedWith listOf(GuessMade(gameId, Guess(guess, feedback)))
    }

    private fun guessExamples(block: (Triple<Code, Code, Feedback>) -> Unit) = mapOf(
        "it gives a black peg for each code peg on the correct position" to Triple(
            Code("Red", "Green", "Blue", "Yellow"),
            Code("Red", "Purple", "Blue", "Purple"),
            Feedback(IN_PROGRESS, BLACK, BLACK)
        ),
        "it gives no black peg for code peg duplicated on a wrong position" to Triple(
            Code("Red", "Green", "Blue", "Yellow"),
            Code("Red", "Red", "Purple", "Purple"),
            Feedback(IN_PROGRESS, BLACK)
        ),
        "it gives a white peg for code peg that is part of the code but is placed on a wrong position" to Triple(
            Code("Red", "Green", "Blue", "Yellow"),
            Code("Purple", "Red", "Purple", "Purple"),
            Feedback(IN_PROGRESS, WHITE)
        ),
        "it gives no white peg for code peg duplicated on a wrong position" to Triple(
            Code("Red", "Green", "Blue", "Yellow"),
            Code("Purple", "Red", "Red", "Purple"),
            Feedback(IN_PROGRESS, WHITE)
        ),
        "it gives a white peg for each code peg on a wrong position" to Triple(
            Code("Red", "Green", "Blue", "Red"),
            Code("Purple", "Red", "Red", "Purple"),
            Feedback(IN_PROGRESS, WHITE, WHITE)
        )
    ).dynamicTestsFor(block)

    @Test
    fun `the game is won if the secret is guessed`() {
        val game = gameOf(GameStarted(gameId, secret, totalAttempts, availablePegs))

        execute(MakeGuess(gameId, secret), game) shouldSucceedWith listOf(
            GuessMade(
                gameId, Guess(
                    secret, Feedback(
                        WON, BLACK, BLACK, BLACK, BLACK
                    )
                )
            ),
            GameWon(gameId)
        )
    }

    @Test
    fun `the game can no longer be played once it's won`() {
        val game = gameOf(GameStarted(gameId, secret, totalAttempts, availablePegs))

        val update = execute(MakeGuess(gameId, secret), game)
        val updatedGame = game.updated(update)

        execute(MakeGuess(gameId, secret), updatedGame) shouldFailWith
                GameAlreadyWon(gameId)
    }

    @Test
    fun `the game is lost if the secret is not guessed within the number of attempts`() {
        val secret = Code("Red", "Green", "Blue", "Yellow")
        val wrongCode = Code("Purple", "Purple", "Purple", "Purple")
        val game = gameOf(
            GameStarted(gameId, secret, 3, availablePegs),
            GuessMade(gameId, Guess(wrongCode, Feedback(IN_PROGRESS))),
            GuessMade(gameId, Guess(wrongCode, Feedback(IN_PROGRESS))),
        )
        execute(MakeGuess(gameId, wrongCode), game) shouldSucceedWith listOf(
            GuessMade(gameId, Guess(wrongCode, Feedback(LOST))),
            GameLost(gameId)
        )
    }

    @Test
    fun `the game can no longer be played once it's lost`() {
        val secret = Code("Red", "Green", "Blue", "Yellow")
        val wrongCode = Code("Purple", "Purple", "Purple", "Purple")
        val game = gameOf(GameStarted(gameId, secret, 1, availablePegs))

        val update = execute(MakeGuess(gameId, wrongCode), game)
        val updatedGame = game.updated(update)

        execute(MakeGuess(gameId, secret), updatedGame) shouldFailWith
                GameAlreadyLost(gameId)
    }

    @Test
    fun `the game cannot be played if it was not started`() {
        val code = Code("Red", "Purple", "Red", "Purple")
        val game = notStartedGame()

        execute(MakeGuess(gameId, code), game) shouldFailWith GameNotStarted(gameId)
    }

    @Test
    fun `the guess length cannot be shorter than the secret`() {
        val secret = Code("Red", "Green", "Blue", "Yellow")
        val code = Code("Purple", "Purple", "Purple")
        val game = gameOf(GameStarted(gameId, secret, 12, availablePegs))

        execute(MakeGuess(gameId, code), game) shouldFailWith GuessTooShort(gameId, code, secret.length)
    }

    @Test
    fun `the guess length cannot be longer than the secret`() {
        val secret = Code("Red", "Green", "Blue", "Yellow")
        val code = Code("Purple", "Purple", "Purple", "Purple", "Purple")
        val game = gameOf(GameStarted(gameId, secret, 12, availablePegs))

        execute(MakeGuess(gameId, code), game) shouldFailWith GuessTooLong(gameId, code, secret.length)
    }

    @Test
    fun `it rejects pegs that the game was not started with`() {
        val secret = Code("Red", "Green", "Blue", "Blue")
        val availablePegs = setOfPegs("Red", "Green", "Blue")
        val game = gameOf(GameStarted(gameId, secret, 12, availablePegs))
        val guess = Code("Red", "Green", "Blue", "Yellow")

        execute(MakeGuess(gameId, guess), game) shouldFailWith
                InvalidPegInGuess(gameId, guess, availablePegs)
    }

    private fun gameOf(vararg events: GameEvent): Game = events.toList().applyTo(NotStartedGame)

    private fun Game.updated(update: Either<GameError, NonEmptyList<GameEvent>>): Game =
        update
            .map { events -> events.applyTo(this) }
            .getOrElse { e -> throw RuntimeException("Expected a list of events but got `$e`.") }

    private fun Iterable<GameEvent>.applyTo(game: Game): Game = fold(game, ::applyEvent)
}
