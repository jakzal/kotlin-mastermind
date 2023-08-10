package mastermind.game.http

import mastermind.game.DecodingBoard
import mastermind.game.GameId
import org.http4k.core.*
import org.http4k.format.Jackson.auto
import org.http4k.lens.Header
import org.http4k.lens.Path
import org.http4k.routing.bind
import org.http4k.routing.routes

interface MastermindApp {
    fun joinGame(): GameId
    fun viewDecodingBoard(gameId: GameId): DecodingBoard?
}

fun mastermindHttpApp(
    app: MastermindApp
) = routes(
    "/games" bind Method.POST to { _: Request ->
        app.joinGame() thenRespond { gameId ->
            Response(Status.CREATED).with(gameId.asLocationHeader())
        }
    },
    "/games/{id}" bind Method.GET to { request ->
        app.viewDecodingBoard(request.id.asGameId()) thenRespond DecodingBoard?::asResponse
    }
)

private val Request.id: String
    get() = Path.of("id")(this)

private fun String.asGameId(): GameId = GameId(this)

private fun GameId.asLocationHeader(): (Response) -> Response = Header.LOCATION of Uri.of("/games/${value}")

private fun DecodingBoard?.asResponse() = this?.let { decodingBoard ->
    Response(Status.OK).with(decodingBoard.asResponseBody())
} ?: Response(Status.NOT_FOUND)

private fun DecodingBoard.asResponseBody(): (Response) -> Response =
    Body.auto<DecodingBoard>().toLens() of this

private infix fun <T> T.thenRespond(responder: (T) -> Response): Response =
    this.let(responder)
