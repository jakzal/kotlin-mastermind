package mastermind.game.http

import arrow.core.Either
import arrow.core.getOrElse
import kotlinx.coroutines.runBlocking
import mastermind.game.*
import mastermind.game.GameCommand.MakeGuess
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
) = with(app) {
    routes(
        "/games" bind Method.POST to { _: Request ->
            runBlocking {
                joinGame() thenRespond GameId::asResponse or JournalFailure<GameError>::response
            }
        },
        "/games/{id}" bind Method.GET to { request ->
            runBlocking {
                viewDecodingBoard(request.id.asGameId()) thenRespond DecodingBoard?::asResponse
            }
        },
        "games/{id}/guesses" bind Method.POST to { request ->
            runBlocking {
                makeGuess(request.makeGuessCommand) thenRespond GameId::asResponse or JournalFailure<GameError>::response
            }
        }
    )
}

private val Request.makeGuessCommand: MakeGuess
    get() = MakeGuess(id.asGameId(), guess)

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

private infix fun <ERROR, RESULT> Either<ERROR, RESULT>.thenRespond(responder: (RESULT) -> Response): Either<ERROR, Response> =
    map(responder)

private infix fun <ERROR> Either<ERROR, Response>.or(errorHandler: (ERROR) -> Response): Response =
    getOrElse(errorHandler)

private fun JournalFailure<GameError>.response(): Response = when (this) {
    is EventStoreFailure<GameError> -> when (this) {
        is StreamNotFound<GameError> -> Response(Status.NOT_FOUND)
        is EventStoreFailure.VersionConflict -> TODO()
    }

    is JournalFailure.ExecutionFailure<GameError> -> this.cause.response()
}

private fun GameError.response() =
    when (this) {
        is GameError.GameFinishedError.GameAlreadyWon -> Response(Status.BAD_REQUEST)
            .with(Body.auto<ErrorResponse>().toLens() of ErrorResponse("Game `${this.gameId.value}` is already won."))

        is GameError.GameFinishedError.GameAlreadyLost -> Response(Status.BAD_REQUEST)
            .with(Body.auto<ErrorResponse>().toLens() of ErrorResponse("Game `${this.gameId.value}` is already lost."))
    }

data class ErrorResponse(val message: String)
