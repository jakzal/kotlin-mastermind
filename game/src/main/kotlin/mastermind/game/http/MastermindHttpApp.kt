package mastermind.game.http

import arrow.core.Either
import arrow.core.getOrElse
import kotlinx.coroutines.runBlocking
import mastermind.game.*
import mastermind.game.Code.Peg
import mastermind.game.GameCommand.MakeGuess
import mastermind.game.view.DecodingBoard
import mastermind.journal.JournalError
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

fun mastermindHttpApp(app: MastermindApp): HttpHandler = app.routes

val MastermindApp.routes: HttpHandler
    get() = routes(
        "/games" bind Method.POST to ::joinGameHandler,
        "/games/{id}" bind Method.GET to ::viewBoardHandler,
        "/games/{id}/guesses" bind Method.POST to ::makeGuessHandler
    )

private fun MastermindApp.joinGameHandler(@Suppress("UNUSED_PARAMETER") request: Request) = runBlocking {
    joinGame() thenRespond GameId::asResponse or handleError
}

private fun MastermindApp.viewBoardHandler(request: Request) = runBlocking {
    viewDecodingBoard(request.gameId) thenRespond DecodingBoard?::asResponse
}

private fun MastermindApp.makeGuessHandler(request: Request) = runBlocking {
    makeGuess(request.makeGuessCommand) thenRespond GameId::asResponse or handleError
}

private val handleError
    get() = JournalError<GameError>::response

private val Request.makeGuessCommand: MakeGuess
    get() = MakeGuess(gameId, guess)

private val Request.gameId: GameId
    get() = Path.of("id")(this).asGameId()

private val Request.guess: Code
    get() = Code(Body.auto<List<String>>().toLens().invoke(this).toCodePegs())

private fun List<String>.toCodePegs(): List<Peg> = map(::Peg)

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
