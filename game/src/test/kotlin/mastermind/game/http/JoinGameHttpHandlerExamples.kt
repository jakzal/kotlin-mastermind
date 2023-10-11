package mastermind.game.http

import arrow.core.right
import mastermind.game.testkit.DirectRunnerModule
import mastermind.game.GameModule
import mastermind.game.GameCommand.JoinGame
import mastermind.game.MastermindApp
import mastermind.game.testkit.anyGameId
import mastermind.testkit.assertions.shouldBe
import mastermind.testkit.assertions.shouldReturn
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Status.Companion.CREATED
import org.junit.jupiter.api.Test

class JoinGameHttpHandlerExamples {
    @Test
    fun `it returns the location of the joined game`() {
        val gameId = anyGameId()
        val app = MastermindApp(
            gameModule = GameModule(
                execute = { command ->
                    when (command) {
                        is JoinGame -> gameId.right()
                        else -> throw RuntimeException("Unexpected command: $command")
                    }
                }
            ),
            runnerModule = DirectRunnerModule()
        ).routes

        val response = app(Request(POST, "/games"))

        response.status shouldBe CREATED
        response.header("Location") shouldReturn "/games/${gameId.value}"
    }
}
