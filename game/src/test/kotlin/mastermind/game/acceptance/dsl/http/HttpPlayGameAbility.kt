package mastermind.game.acceptance.dsl.http

import arrow.core.Either
import mastermind.game.Code
import mastermind.game.GameError
import mastermind.game.GameId
import mastermind.game.acceptance.dsl.PlayGameAbility
import mastermind.game.journal.JournalFailure
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
        Assertions.assertEquals(Status.OK, response.status)
        return Body.auto<DecodingBoard>().toLens()(response)
    }

    override suspend fun makeGuess(gameId: GameId, code: Code): Either<JournalFailure<GameError>, GameId> {
        TODO("Not yet implemented")
    }
}