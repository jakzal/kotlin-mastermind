package mastermind.game.acceptance

import kotlinx.coroutines.runBlocking
import mastermind.game.*
import mastermind.game.http.mastermindHttpApp
import mastermind.game.testkit.anySecret
import mastermind.game.testkit.shouldBe
import mastermind.game.view.DecodingBoard
import org.http4k.client.ApacheClient
import org.http4k.core.Body
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Status.Companion.CREATED
import org.http4k.core.Status.Companion.OK
import org.http4k.format.Jackson.auto
import org.http4k.server.Undertow
import org.http4k.server.asServer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test

class JoiningTheGameExamples {
    @Test
    fun `code breaker joins the game`() = MastermindScenario(
        // Given a decoding board of 12 attempts
        totalAttempts = 12,
        // And the code maker has placed a secret on the board
        secret = anySecret()
    ) {
        startApplication(totalAttempts, secret)
        // When I join the game
        joinGame { gameId ->
            // Then the game should be started with an empty board
            // And I should have 12 attempts available
            viewDecodingBoard(gameId) shouldBe DecodingBoard(
                gameId.value,
                secret.size,
                totalAttempts,
                emptyList(),
                "In progress"
            )
        }
    }
}

class MastermindScenario(
    val secret: Code,
    val totalAttempts: Int
) {
    companion object {
        operator fun invoke(
            secret: Code,
            totalAttempts: Int = 12,
            scenario: suspend MastermindScenario.() -> Unit
        ) {
            runBlocking {
                MastermindScenario(secret, totalAttempts).scenario()
            }
        }
    }
}

val server = mastermindHttpApp(MastermindApp(
    configuration = Configuration(
        // @TODO secret made by makeCode() here is not the same as the one in the test. The test passes as the code is the same length.
        codeMaker = { makeCode() }
    )
)).asServer(Undertow(0)).start()
val client = ApacheClient()

private fun startApplication(totalAttempts: Int, secret: Code) {
}

private fun joinGame(block: (GameId) -> Unit) {
    val response = client(Request(POST, "http://localhost:${server.port()}/games"))
    assertEquals(CREATED, response.status)
    response.header("Location")
        ?.substringAfter("/games/", "")
        ?.let(::GameId)
        ?.also(block) ?: fail("Location header not found in the response.")
}

private fun viewDecodingBoard(gameId: GameId): DecodingBoard {
    val response = client(Request(GET, "http://localhost:${server.port()}/games/${gameId.value}"))
    assertEquals(OK, response.status)
    return Body.auto<DecodingBoard>().toLens()(response)
}

