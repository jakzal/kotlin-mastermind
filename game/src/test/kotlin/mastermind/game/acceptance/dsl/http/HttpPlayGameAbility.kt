package mastermind.game.acceptance.dsl.http

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.right
import mastermind.game.Code
import mastermind.game.GameError
import mastermind.game.GameError.GameFinishedError.GameAlreadyLost
import mastermind.game.GameError.GameFinishedError.GameAlreadyWon
import mastermind.game.GameId
import mastermind.game.acceptance.dsl.PlayGameAbility
import mastermind.game.http.Error
import mastermind.game.view.DecodingBoard
import mastermind.journal.JournalFailure
import mastermind.journal.JournalFailure.ExecutionFailure
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
        Assertions.assertEquals(Status.OK, response.status)
        return Body.auto<DecodingBoard>().toLens()(response)
    }

    override suspend fun makeGuess(gameId: GameId, code: Code): Either<JournalFailure<GameError>, GameId> {
        val response = client(
            Request(Method.POST, "http://localhost:$serverPort/games/${gameId.value}/guesses")
                .body(code.pegs)
        )
        return if (response.status.successful) {
            gameId.right()
        } else {
            response.executionFailure(gameId)
        }
    }
}

private fun Response.executionFailure(gameId: GameId): Either<JournalFailure<GameError>, GameId> = either {
    if (body().message.matches(".*won.*".toRegex())) {
        raise(ExecutionFailure(GameAlreadyWon(gameId)))
    } else {
        raise(ExecutionFailure(GameAlreadyLost(gameId)))
    }
}

private fun Request.body(pegs: List<String>): Request = Body.auto<List<String>>().toLens().invoke(pegs, this)
private fun Response.body(): Error = Body.auto<Error>().toLens().extract(this)
