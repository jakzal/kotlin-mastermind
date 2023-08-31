package mastermind.game

import arrow.core.nonEmptyListOf
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
        val game = nonEmptyListOf(GameStarted(gameId, secret, totalAttempts))

        val updatedGame = execute(MakeGuess(gameId, secret), game)

        execute(MakeGuess(gameId, secret), updatedGame.getOrNull()) shouldFailWith
                GameFinishedFailure.GameWonFailure(gameId)
    }
}
