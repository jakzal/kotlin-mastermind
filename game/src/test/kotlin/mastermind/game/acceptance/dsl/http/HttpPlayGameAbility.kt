package mastermind.game.acceptance.dsl.http

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.right
import mastermind.eventstore.EventStoreError
import mastermind.eventstore.EventStoreError.ExecutionError
import mastermind.game.Code
import mastermind.game.GameError
import mastermind.game.GameError.GameFinishedError.GameAlreadyLost
import mastermind.game.GameError.GameFinishedError.GameAlreadyWon
import mastermind.game.GameId
import mastermind.game.acceptance.dsl.PlayGameAbility
import mastermind.game.http.Error
import mastermind.game.view.DecodingBoard
import org.http4k.client.ApacheClient
import org.http4k.core.*
import org.http4k.format.Jackson.auto
import org.junit.jupiter.api.Assertions

class HttpPlayGameAbility(
    private val serverPort: Int,
    private val client: HttpHandler = ApacheClient()
) : PlayGameAbility {

    override suspend fun joinGame(onceJoined: suspend PlayGameAbility.(GameId) -> Unit) {
        val response = client(Request(Method.POST, "http://localhost:$serverPort/games"))
        Assertions.assertEquals(Status.CREATED, response.status)
        response.header("Location")
            ?.substringAfter("/games/", "")
            ?.let(::GameId)
            ?.also { this.onceJoined(it) } ?: Assertions.fail("Location header not found in the response.")
    }

    override suspend fun viewDecodingBoard(gameId: GameId): DecodingBoard? {
        val response = client(Request(Method.GET, "http://localhost:$serverPort/games/${gameId.value}"))
        return if (response.status.successful) {
            Body.auto<DecodingBoard>().toLens()(response)
        } else {
            null
        }
    }

    override suspend fun makeGuess(gameId: GameId, code: Code): Either<EventStoreError<GameError>, GameId> {
        val response = client(
            Request(Method.POST, "http://localhost:$serverPort/games/${gameId.value}/guesses")
                .body(code.pegs)
        )
        return if (response.status.successful) {
            gameId.right()
        } else {
            response.executionError(gameId)
        }
    }
}

private fun Response.executionError(gameId: GameId): Either<EventStoreError<GameError>, GameId> = either {
    if (body().message.matches(".*won.*".toRegex())) {
        raise(ExecutionError(GameAlreadyWon(gameId)))
    } else {
        raise(ExecutionError(GameAlreadyLost(gameId)))
    }
}

private fun Request.body(pegs: List<Code.Peg>): Request = Body.auto<List<String>>().toLens().invoke(pegs.map(Code.Peg::name), this)
private fun Response.body(): Error = Body.auto<Error>().toLens().extract(this)
