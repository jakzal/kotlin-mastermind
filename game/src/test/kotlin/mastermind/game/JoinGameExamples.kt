package mastermind.game

import arrow.core.raise.either
import arrow.core.right
import kotlinx.coroutines.test.runTest
import mastermind.game.testkit.anyGameId
import mastermind.game.testkit.anySecret
import mastermind.game.testkit.shouldBe
import org.junit.jupiter.api.Test

class JoinGameExamples {

    @Test
    fun `it sends the JoinGame command to the game command handler`() = runTest {
        val gameId = anyGameId()
        val secret = anySecret()
        val totalAttempts = 12
        val gameCommandHandler: GameCommandHandler = { command: GameCommand ->
            either {
                command.gameId.also {
                    command shouldBe JoinGame(gameId, secret, totalAttempts)
                }
            }
        }

        val result = with(GameIdGenerator { gameId }) {
            with(CodeMaker { secret }) {
                with(gameCommandHandler) {
                    joinGame()
                }
            }
        }

        result shouldBe gameId.right()
    }
}