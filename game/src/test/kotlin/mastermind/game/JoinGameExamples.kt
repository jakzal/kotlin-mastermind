package mastermind.game

import kotlinx.coroutines.test.runTest
import mastermind.game.testkit.shouldBe
import org.junit.jupiter.api.Test

class JoinGameExamples {

    context(GameIdGenerator, CodeMaker, GameCommandHandler)
    suspend fun joinGame(): GameId =
        this@GameCommandHandler(
            JoinGame(
                this@GameIdGenerator(),
                this@CodeMaker(),
                12
            )
        )

    @Test
    fun `it sends the JoinGame command to the game command handler`() = runTest {
        val gameId = anyGameId()
        val secret = anySecret()
        val totalAttempts = 12
        val gameCommandHandler: suspend (JoinGame) -> GameId = { command ->
            command.gameId.also {
                command shouldBe JoinGame(gameId, secret, totalAttempts)
            }
        }

        val result = with({ gameId }) {
            with({ secret }) {
                with(gameCommandHandler) {
                    joinGame()
                }
            }
        }

        result shouldBe gameId
    }

}

typealias CodeMaker = () -> Code

typealias GameIdGenerator = () -> GameId

typealias GameCommandHandler = suspend (JoinGame) -> GameId

private fun anySecret(): Code = Code("Red", "Blue", "Purple", "Red")

private fun anyGameId(): GameId = generateGameId()

data class JoinGame(val gameId: GameId, val secret: Code, val totalAttempts: Int)
