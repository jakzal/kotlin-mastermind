package mastermind.game.http

import arrow.core.Either
import kotlinx.coroutines.runBlocking
import mastermind.game.Code
import mastermind.game.GameError
import mastermind.game.GameId
import mastermind.game.MastermindApp
import mastermind.game.view.DecodingBoard
import mastermind.journal.JournalFailure
import mastermind.journal.JournalFailure.EventStoreFailure
import mastermind.journal.JournalFailure.EventStoreFailure.StreamNotFound
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
            app.joinGame() thenRespond GameId::asResponse
        }
    },
    "/games/{id}" bind Method.GET to { request ->
        runBlocking {
            app.viewDecodingBoard(request.id.asGameId()) thenRespond DecodingBoard?::asResponse
        }
    },
    "games/{id}/guesses" bind Method.POST to { request ->
        runBlocking {
            app.makeGuess(request.id.asGameId(), request.guess) thenRespond GameId::asResponse
        }
    }
)

private val Request.id: String
    get() = Path.of("id")(this)

private val Request.guess: Code
    get() = Code(Body.auto<List<String>>().toLens().invoke(this))

private fun String.asGameId(): GameId = GameId(this)

private fun GameId.asLocationHeader(): (Response) -> Response = Header.LOCATION of Uri.of("/games/${value}")

private fun DecodingBoard?.asResponse() = this?.let { decodingBoard ->
    Response(Status.OK).with(decodingBoard.asResponseBody())
} ?: Response(Status.NOT_FOUND)

private fun DecodingBoard.asResponseBody(): (Response) -> Response =
    Body.auto<DecodingBoard>().toLens() of this

private fun GameId.asResponse() = Response(Status.CREATED).with(this.asLocationHeader())

private infix fun <T> T.thenRespond(responder: (T) -> Response): Response =
    this.let(responder)

private infix fun Either<JournalFailure<GameError>, GameId>.thenRespond(responder: (GameId) -> Response): Response =
    fold(JournalFailure<GameError>::response, responder)

private fun JournalFailure<GameError>.response(): Response = when(this) {
    is EventStoreFailure<GameError> -> when (this) {
        is StreamNotFound<GameError> -> Response(Status.NOT_FOUND)
        is EventStoreFailure.VersionConflict -> TODO()
    }

    is JournalFailure.ExecutionFailure -> TODO()
}
