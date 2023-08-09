package mastermind.game.http

import org.http4k.client.ApacheClient
import org.http4k.core.*
import org.http4k.core.Method.GET
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.http4k.format.Jackson.auto
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.server.Undertow
import org.http4k.server.asServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test


/**
 * @see <a href="https://www.http4k.org/quickstart/">http4k quickstart guide</a>
 */
@Tag("learning")
class LearnHttp4kExamples {
    private val client = ApacheClient()
    private val server = routes(
        "/hello" bind GET to { request ->
            Response(OK).with(
                Greeting("Hello, ${request.query("name") ?: "anonymous"}!").asHttpMessage()
            )
        }
    ).asServer(Undertow(0))

    @BeforeEach
    fun startServer() {
        server.start()
    }

    @AfterEach
    fun stopServer() {
        server.stop()
    }

    @Test
    fun `client calls the server`() {
        val response = client(Request(GET, "http://localhost:${server.port()}/hello"))

        assertEquals(OK, response.status)
        assertEquals(Greeting("Hello, anonymous!"), response.asGreeting())
    }

    @Test
    fun `client calls a route not found on the server`() {
        val response = client(Request(GET, "http://localhost:${server.port()}/not-found"))

        assertEquals(NOT_FOUND, response.status)
    }
}

private fun Greeting.asHttpMessage(): (Response) -> Response = Body.auto<Greeting>().toLens() of this

private fun HttpMessage.asGreeting(): Greeting = Body.auto<Greeting>().toLens()(this)

private data class Greeting(val text: String)