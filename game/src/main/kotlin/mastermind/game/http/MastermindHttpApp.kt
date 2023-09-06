package mastermind.game.http

import arrow.core.Either
import arrow.core.getOrElse
import kotlinx.coroutines.runBlocking
import mastermind.game.*
import mastermind.game.Code.Peg
import mastermind.game.GameCommand.MakeGuess
import mastermind.game.view.DecodingBoard
import mastermind.journal.JournalFailure
import org.http4k.core.*
import org.http4k.format.Jackson.auto
import org.http4k.lens.Header
import org.http4k.lens.Path
import org.http4k.routing.RoutingHttpHandler
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
    app: MastermindApp,
    handleFailure: (JournalFailure<GameError>) -> Response = JournalFailure<GameError>::response
): RoutingHttpHandler {
    return routes(
        "/games" bind Method.POST to app.handler {
            joinGame() thenRespond GameId::asResponse or handleFailure
        },
        "/games/{id}" bind Method.GET to app.handler { request ->
            viewDecodingBoard(request.id.asGameId()) thenRespond DecodingBoard?::asResponse
        },
        "games/{id}/guesses" bind Method.POST to app.handler { request ->
            makeGuess(request.makeGuessCommand) thenRespond GameId::asResponse or handleFailure
        }
    )
}

private fun MastermindApp.handler(handle: suspend MastermindApp.(Request) -> Response) = { request: Request ->
    run {
        runBlocking {
            handle(request)
        }
    }
}

private val Request.makeGuessCommand: MakeGuess
    get() = MakeGuess(id.asGameId(), guess)

private val Request.id: String
    get() = Path.of("id")(this)

private val Request.guess: Code
    get() = Code(Body.auto<List<String>>().toLens().invoke(this).toCodePegs())

private fun List<String>.toCodePegs(): List<Peg> = map(String::uppercase).map { Peg(it) }

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

private infix fun <ERROR, RESULT> Either<ERROR, RESULT>.thenRespond(responder: (RESULT) -> Response): Either<ERROR, Response> =
    map(responder)

private infix fun <ERROR> Either<ERROR, Response>.or(errorHandler: (ERROR) -> Response): Response =
    getOrElse(errorHandler)
