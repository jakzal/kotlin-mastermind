package mastermind.game.acceptance

import kotlinx.coroutines.runBlocking
import mastermind.game.*
import mastermind.game.http.mastermindHttpApp
import mastermind.game.testkit.anySecret
import mastermind.game.testkit.shouldBe
import mastermind.game.view.DecodingBoard
import org.http4k.client.ApacheClient
import org.http4k.core.Body
import org.http4k.core.HttpHandler
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
    private val ability: PlayGameAbility,
    val secret: Code,
    val totalAttempts: Int
) : PlayGameAbility by ability {
    companion object {
        operator fun invoke(
            secret: Code,
            totalAttempts: Int = 12,
            scenario: suspend MastermindScenario.() -> Unit
        ) {
            val server = mastermindHttpApp(MastermindApp(
                configuration = Configuration(
                    codeMaker = { secret }
                )
            )).asServer(Undertow(0)).start()
            runBlocking {
                MastermindScenario(HttpPlayGameAbility(server.port()), secret, totalAttempts).scenario()
            }
        }
    }
}

interface PlayGameAbility {
    suspend fun joinGame(block: suspend PlayGameAbility.(GameId) -> Unit)
    suspend fun viewDecodingBoard(gameId: GameId): DecodingBoard
}

class HttpPlayGameAbility(
    private val serverPort: Int,
    private val client: HttpHandler = ApacheClient()
) : PlayGameAbility {

    override suspend fun joinGame(block: suspend PlayGameAbility.(GameId) -> Unit) {
        val response = client(Request(POST, "http://localhost:$serverPort/games"))
        assertEquals(CREATED, response.status)
        response.header("Location")
            ?.substringAfter("/games/", "")
            ?.let(::GameId)
            ?.also { this.block(it) } ?: fail("Location header not found in the response.")
    }

    override suspend fun viewDecodingBoard(gameId: GameId): DecodingBoard {
        val response = client(Request(GET, "http://localhost:$serverPort/games/${gameId.value}"))
        assertEquals(OK, response.status)
        return Body.auto<DecodingBoard>().toLens()(response)
    }
}
