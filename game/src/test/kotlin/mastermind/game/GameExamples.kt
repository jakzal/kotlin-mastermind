package mastermind.game

import arrow.core.nonEmptyListOf
import arrow.core.right
import mastermind.game.testkit.anyGameId
import mastermind.game.testkit.anySecret
import mastermind.game.testkit.shouldReturn
import org.junit.jupiter.api.Test

class GameExamples {
    @Test
    fun `it executes the JoinGame command`() {
        val gameId = anyGameId()
        val secret = anySecret()
        val totalAttempts = 12
        execute(JoinGame(gameId, secret, totalAttempts)) shouldReturn
                listOf(GameStarted(gameId, secret, totalAttempts)).right()
    }

    @Test
    fun `it executes the MakeGuess command`() {
        val gameId = anyGameId()
        val secret = Code("Red", "Green", "Blue", "Yellow")
        val totalAttempts = 12

        val game = nonEmptyListOf(GameStarted(gameId, secret, totalAttempts))

        execute(MakeGuess(gameId, Code("Purple", "Purple", "Purple", "Purple")), game) shouldReturn
                listOf(
                    GuessMade(
                        gameId,
                        Guess(
                            Code("Purple", "Purple", "Purple", "Purple"),
                            Feedback(emptyList(), Feedback.Outcome.IN_PROGRESS)
                        )
                    )
                ).right()
    }
}
