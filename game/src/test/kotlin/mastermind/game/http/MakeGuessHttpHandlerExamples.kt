package mastermind.game.http

import arrow.core.left
import arrow.core.right
import mastermind.game.Code
import mastermind.game.GameCommand.MakeGuess
import mastermind.game.GameError
import mastermind.game.MastermindApp
import mastermind.game.testkit.anyGameId
import mastermind.game.testkit.shouldBe
import mastermind.journal.JournalFailure.EventStoreFailure.StreamNotFound
import org.http4k.core.Body
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Status
import org.http4k.format.Jackson.auto
import org.junit.jupiter.api.Test

class MakeGuessHttpHandlerExamples {
    @Test
    fun `it returns the location of the game after making a guess`() {
        val gameId = anyGameId()
        val guess = Code("Red", "Green", "Green", "Yellow")
        val app = mastermindHttpApp(MastermindApp(
            makeGuessUseCase = { command: MakeGuess ->
                command shouldBe MakeGuess(gameId, guess)
                gameId.right()
            }
        ))

        val response = app(Request(Method.POST, "/games/${gameId.value}/guesses").body(guess.pegs))

        response.status shouldBe Status.CREATED
        response.header("Location") shouldBe "/games/${gameId.value}"
    }

    @Test
    fun `it returns 404 if the game does not exist`() {
        val gameId = anyGameId()
        val guess = Code("Red", "Green", "Green", "Yellow")
        val app = mastermindHttpApp(MastermindApp(
            makeGuessUseCase = { _: MakeGuess ->
                StreamNotFound<GameError>("my-stream").left()
            }
        ))

        val response = app(Request(Method.POST, "/games/${gameId.value}/guesses").body(guess.pegs))

        response.status shouldBe Status.NOT_FOUND
    }
}

private fun Request.body(pegs: List<String>): Request = Body.auto<List<String>>().toLens().invoke(pegs, this)
