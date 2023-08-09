package mastermind.game.http

import org.http4k.client.ApacheClient
import org.http4k.core.Body
import org.http4k.core.ContentType
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.http4k.lens.string
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.server.Undertow
import org.http4k.server.asServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test


/**
 * @see <a href="https://www.http4k.org/quickstart/">http4k quickstart guide</a>
 */
class LearnHttp4kExamples {
    private val client = ApacheClient()
    private val server = routes(
        "/hello" bind GET to { request ->
            Response(OK).body("Hello, ${request.query("name") ?: "anonymous"}!")
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
        assertEquals("Hello, anonymous!", Body.string(ContentType.TEXT_PLAIN).toLens()(response))
    }

    @Test
    fun `client calls a route not found on the server`() {
        val response = client(Request(GET, "http://localhost:${server.port()}/not-found"))

        assertEquals(NOT_FOUND, response.status)
    }
}