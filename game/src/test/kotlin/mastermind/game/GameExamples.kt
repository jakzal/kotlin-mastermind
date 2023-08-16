package mastermind.game

import arrow.core.nonEmptyListOf
import mastermind.game.testkit.anyGameId
import mastermind.game.testkit.anySecret
import mastermind.game.testkit.shouldSucceedWith
import org.junit.jupiter.api.Test

class GameExamples {
    @Test
    fun `it executes the JoinGame command`() {
        val gameId = anyGameId()
        val secret = anySecret()
        val totalAttempts = 12
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
        val gameId = anyGameId()
        val secret = Code("Red", "Green", "Blue", "Yellow")
        val totalAttempts = 12

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
}
