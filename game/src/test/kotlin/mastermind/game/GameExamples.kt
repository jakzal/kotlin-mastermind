package mastermind.game

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
}
