package mastermind.game

import arrow.core.nonEmptyListOf
import mastermind.game.testkit.anyGameId
import mastermind.game.testkit.shouldSucceedWith
import org.junit.jupiter.api.Test

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

    @Test
    fun `it gives black peg for each code peg on the correct position`() {
        val secret = Code("Red", "Green", "Blue", "Yellow")
        val game = nonEmptyListOf(GameStarted(gameId, secret, totalAttempts))

        execute(MakeGuess(gameId, Code("Red", "Purple", "Blue", "Purple")), game) shouldSucceedWith listOf(
            GuessMade(
                gameId,
                Guess(
                    Code("Red", "Purple", "Blue", "Purple"),
                    Feedback(listOf("Black", "Black"), Feedback.Outcome.IN_PROGRESS)
                )
            )
        )
    }
}
