package mastermind.game

import arrow.core.getOrElse
import arrow.core.nonEmptyListOf
import mastermind.game.GameCommand.JoinGame
import mastermind.game.GameCommand.MakeGuess
import mastermind.game.GameError.GameFinishedError.GameAlreadyLost
import mastermind.game.GameError.GameFinishedError.GameAlreadyWon
import mastermind.game.GameError.GuessError.GameNotStarted
import mastermind.game.GameError.GuessError.GuessTooShort
import mastermind.game.GameEvent.*
import mastermind.game.testkit.anyGameId
import mastermind.game.testkit.shouldFailWith
import mastermind.game.testkit.shouldSucceedWith
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

class GameExamples {
    private val gameId = anyGameId()
    private val secret = Code("Red", "Green", "Blue", "Yellow")
    private val totalAttempts = 12

    @Test
    fun `it executes the JoinGame command`() {
        execute(JoinGame(gameId, secret, totalAttempts)) shouldSucceedWith listOf(
            GameStarted(
                gameId,
                secret,
                totalAttempts
            )
        )
    }

    @Test
    fun `it executes the MakeGuess command`() {
        val game = nonEmptyListOf(GameStarted(gameId, secret, totalAttempts))

        execute(MakeGuess(gameId, Code("Purple", "Purple", "Purple", "Purple")), game) shouldSucceedWith listOf(
            GuessMade(
                gameId,
                Guess(
                    Code("Purple", "Purple", "Purple", "Purple"),
                    Feedback(emptyList(), Feedback.Outcome.IN_PROGRESS)
                )
            )
        )
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("guessExamples")
    fun `it gives feedback on the guess`(message: String, secret: Code, guess: Code, feedback: Feedback) {
        val game = nonEmptyListOf(GameStarted(gameId, secret, totalAttempts))

        execute(MakeGuess(gameId, guess), game) shouldSucceedWith listOf(GuessMade(gameId, Guess(guess, feedback)))
    }

    companion object {
        @JvmStatic
        fun guessExamples(): List<Arguments> = listOf(
            Arguments.of(
                "it gives a black peg for each code peg on the correct position",
                Code("Red", "Green", "Blue", "Yellow"),
                Code("Red", "Purple", "Blue", "Purple"),
                Feedback(listOf("Black", "Black"), Feedback.Outcome.IN_PROGRESS)
            ),
            Arguments.of(
                "it gives no black peg for code peg duplicated on a wrong position",
                Code("Red", "Green", "Blue", "Yellow"),
                Code("Red", "Red", "Purple", "Purple"),
                Feedback(listOf("Black"), Feedback.Outcome.IN_PROGRESS)
            ),
            Arguments.of(
                "it gives a white peg for code peg that is part of the code but is placed on a wrong position",
                Code("Red", "Green", "Blue", "Yellow"),
                Code("Purple", "Red", "Purple", "Purple"),
                Feedback(listOf("White"), Feedback.Outcome.IN_PROGRESS)
            ),
            Arguments.of(
                "it gives no white peg for code peg duplicated on a wrong position",
                Code("Red", "Green", "Blue", "Yellow"),
                Code("Purple", "Red", "Red", "Purple"),
                Feedback(listOf("White"), Feedback.Outcome.IN_PROGRESS)
            ),
            Arguments.of(
                "it gives a white peg for each code peg on a wrong position",
                Code("Red", "Green", "Blue", "Red"),
                Code("Purple", "Red", "Red", "Purple"),
                Feedback(listOf("White", "White"), Feedback.Outcome.IN_PROGRESS)
            )
        )
    }

    @Test
    fun `the game is won if the secret is guessed`() {
        val game = nonEmptyListOf(GameStarted(gameId, secret, totalAttempts))

        execute(MakeGuess(gameId, secret), game) shouldSucceedWith listOf(
            GuessMade(
                gameId, Guess(
                    secret, Feedback(
                        listOf("Black", "Black", "Black", "Black"), Feedback.Outcome.WON
                    )
                )
            ),
            GameWon(gameId)
        )
    }

    @Test
    fun `the game can no longer be played once it's won`() {
        val game = nonEmptyListOf<GameEvent>(GameStarted(gameId, secret, totalAttempts))

        val update = execute(MakeGuess(gameId, secret), game)
        val updatedGame = game + update.getOrElse { emptyList() }

        execute(MakeGuess(gameId, secret), updatedGame) shouldFailWith
                GameAlreadyWon(gameId)
    }

    @Test
    fun `the game is lost if the secret is not guessed within the number of attempts`() {
        val wrongCode = Code("Purple", "Purple", "Purple", "Purple")
        val game = nonEmptyListOf(
            GameStarted(gameId, secret, 3),
            GuessMade(gameId, Guess(wrongCode, Feedback(listOf(), Feedback.Outcome.IN_PROGRESS))),
            GuessMade(gameId, Guess(wrongCode, Feedback(listOf(), Feedback.Outcome.IN_PROGRESS))),
        )
        execute(MakeGuess(gameId, wrongCode), game) shouldSucceedWith listOf(
            GuessMade(gameId, Guess(wrongCode, Feedback(listOf(), Feedback.Outcome.LOST))),
            GameLost(gameId)
        )
    }

    @Test
    fun `the game can no longer be played once it's lost`() {
        val wrongCode = Code("Purple", "Purple", "Purple", "Purple")
        val game = nonEmptyListOf<GameEvent>(GameStarted(gameId, secret, 1))

        val update = execute(MakeGuess(gameId, wrongCode), game)
        val updatedGame = game + update.getOrElse { emptyList() }

        execute(MakeGuess(gameId, secret), updatedGame) shouldFailWith
                GameAlreadyLost(gameId)
    }

    @Test
    fun `the game cannot be played if it was not started`() {
        val code = Code("Red", "Purple", "Red", "Purple")
        val game = null

        execute(MakeGuess(gameId, code), game) shouldFailWith GameNotStarted(gameId)
    }

    @Test
    fun `the guess size cannot be longer than the secret`() {
        val code = Code("Purple", "Purple", "Purple")
        val game = nonEmptyListOf<GameEvent>(GameStarted(gameId, secret, 1))

        execute(MakeGuess(gameId, code), game) shouldFailWith GuessTooShort(gameId, code, secret.length)
    }
}
