package mastermind.game.acceptance

import mastermind.game.*
import mastermind.game.http.mastermindHttpApp
import mastermind.game.testkit.anySecret
import mastermind.game.testkit.shouldBe
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
    fun `code breaker joins the game`() {
        // Given a decoding board of 12 attempts
        val totalAttempts = 12
        // And the code maker has placed a secret on the board
        val secret = anySecret()
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

val server = mastermindHttpApp(MastermindApp(
    joinGame = {
        with(GameIdGenerator { GameId("6e252c79-4d02-4b05-92ac-6040e8c7f057") }) {
            with(CodeMaker { makeCode() }) {
                with(GameCommandHandler { it.gameId }) {
                    joinGame()
                }
            }
        }
    },
    viewDecodingBoard = { gameId: GameId -> DecodingBoard(gameId.value, 4, 12, emptyList(), "In progress") }
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

