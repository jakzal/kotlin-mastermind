package mastermind.game

import kotlinx.coroutines.test.runTest
import mastermind.game.testkit.anyGameId
import mastermind.game.testkit.anySecret
import mastermind.game.testkit.shouldBe
import org.junit.jupiter.api.Test

class JoinGameExamples {

    context(GameIdGenerator, CodeMaker, GameCommandHandler)
    suspend fun joinGame(): GameId = handle(JoinGame(generateGameId(), makeCode(), 12))

    @Test
    fun `it sends the JoinGame command to the game command handler`() = runTest {
        val gameId = anyGameId()
        val secret = anySecret()
        val totalAttempts = 12
        val gameCommandHandler = GameCommandHandler { command ->
            command.gameId.also {
                command shouldBe JoinGame(gameId, secret, totalAttempts)
            }
        }

        val result = with(GameIdGenerator { gameId }) {
            with(CodeMaker { secret }) {
                with(gameCommandHandler) {
                    joinGame()
                }
            }
        }

        result shouldBe gameId
    }

}

fun interface CodeMaker {
    fun makeCode(): Code
}

fun interface GameIdGenerator {
    fun generateGameId(): GameId
}

fun interface GameCommandHandler {
    suspend fun handle(command: JoinGame): GameId
}

data class JoinGame(val gameId: GameId, val secret: Code, val totalAttempts: Int)
