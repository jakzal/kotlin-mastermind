package mastermind.game.http

import arrow.core.Either
import kotlinx.coroutines.runBlocking
import mastermind.game.GameFailure
import mastermind.game.GameId
import mastermind.game.MastermindApp
import mastermind.game.journal.JournalFailure
import mastermind.game.view.DecodingBoard
import org.http4k.core.*
import org.http4k.format.Jackson.auto
import org.http4k.lens.Header
import org.http4k.lens.Path
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.server.Undertow
import org.http4k.server.asServer

fun main() {
    mastermindHttpApp(MastermindApp())
        .asServer(Undertow(8080))
        .start()
}

fun mastermindHttpApp(
    app: MastermindApp
) = routes(
    "/games" bind Method.POST to { _: Request ->
        runBlocking {
            with(app.configuration) {
                app.joinGame() thenRespond { gameId ->
                    Response(Status.CREATED).with(gameId.asLocationHeader())
                }
            }
        }
    },
    "/games/{id}" bind Method.GET to { request ->
        runBlocking {
            with(app.configuration) {
                app.viewDecodingBoard(request.id.asGameId()) thenRespond DecodingBoard?::asResponse
            }
        }
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

private infix fun Either<JournalFailure<GameFailure>, GameId>.thenRespond(responder: (GameId) -> Response): Response =
    fold(JournalFailure<GameFailure>::response, responder)

private fun JournalFailure<GameFailure>.response(): Response = TODO("Error handling")
