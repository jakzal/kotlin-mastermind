package mastermind.game.http

import arrow.core.right
import mastermind.game.MastermindApp
import mastermind.game.testkit.anyGameId
import mastermind.game.testkit.shouldBe
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Status.Companion.CREATED
import org.junit.jupiter.api.Test

class JoinGameHttpHandlerExamples {
    @Test
    fun `it returns the location of the joined game`() {
        val gameId = anyGameId()
        val app = mastermindHttpApp(MastermindApp(
            joinGameUseCase = { gameId.right() }
        ))

        val response = app(Request(POST, "/games"))

        response.status shouldBe CREATED
        response.header("Location") shouldBe "/games/${gameId.value}"
    }
}
