package mastermind.game

import arrow.core.nonEmptyListOf
import mastermind.game.testkit.anyGameId
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
        val secret = secret
        val game = nonEmptyListOf(GameStarted(gameId, secret, totalAttempts))

        execute(MakeGuess(gameId, guess), game) shouldSucceedWith listOf(GuessMade(gameId, Guess(guess, feedback)))
    }

    companion object {
        @JvmStatic
        fun guessExamples(): List<Arguments> = listOf(
            Arguments.of(
                "it gives black peg for each code peg on the correct position",
                Code("Red", "Green", "Blue", "Yellow"),
                Code("Red", "Purple", "Blue", "Purple"),
                Feedback(listOf("Black", "Black"), Feedback.Outcome.IN_PROGRESS)
            )
        )
    }
}
