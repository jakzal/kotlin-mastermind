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

private val MastermindApp.routes: HttpHandler
    get() = routes(
        "/games" bind Method.POST to handler {
            joinGame()
                .asResponse()
                .orHandleError()
        },
        "/games/{id}" bind Method.GET to handler { request: Request ->
            viewDecodingBoard(request.gameId)
                .asResponse()
        },
        "/games/{id}/guesses" bind Method.POST to handler { request: Request ->
            makeGuess(request.makeGuessCommand)
                .asResponse()
                .orHandleError()
        }
    )

private fun MastermindApp.handler(handle: suspend MastermindApp.(Request) -> Response) = { request: Request ->
    runBlocking {
        handle(request)
    }
}

private val Request.makeGuessCommand: MakeGuess
    get() = MakeGuess(gameId, guess)

private val Request.gameId: GameId
    get() = Path.of("id")(this).asGameId()

private val Request.guess: Code
    get() = Code(Body.auto<List<String>>().toLens().invoke(this).asCodePegs())

private fun List<String>.asCodePegs(): List<Peg> = map(::Peg)

private fun String.asGameId(): GameId = GameId(this)

private fun GameId.asLocationHeader(): (Response) -> Response = Header.LOCATION of Uri.of("/games/${value}")

private fun DecodingBoard?.asResponse() = this?.let { decodingBoard ->
    Response(Status.OK).with(decodingBoard.asResponseBody())
} ?: Response(Status.NOT_FOUND)

private fun DecodingBoard.asResponseBody(): (Response) -> Response =
    Body.auto<DecodingBoard>().toLens() of this

private fun <ERROR> Either<ERROR, GameId>.asResponse() = map { gameId ->
    Response(Status.CREATED).with(gameId.asLocationHeader())
}

private fun Either<JournalError<GameError>, Response>.orHandleError() =
    getOrElse(JournalError<GameError>::response)
