package mastermind.game.http

import arrow.core.left
import arrow.core.right
import mastermind.game.Code
import mastermind.game.testkit.DirectRunnerModule
import mastermind.game.GameModule
import mastermind.game.GameCommand.MakeGuess
import mastermind.game.MastermindApp
import mastermind.game.testkit.anyGameId
import mastermind.journal.JournalError.StreamNotFound
import mastermind.testkit.assertions.shouldBe
import mastermind.testkit.assertions.shouldReturn
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
        val app = MastermindApp(
            gameModule = GameModule(
                execute = { command ->
                    command shouldBe MakeGuess(gameId, guess)
                    command.gameId.right()
                }
            ),
            runnerModule = DirectRunnerModule()
        ).routes

        val response = app(Request(Method.POST, "/games/${gameId.value}/guesses").body(guess.pegs))

        response.status shouldBe Status.CREATED
        response.header("Location") shouldReturn "/games/${gameId.value}"
    }

    @Test
    fun `it returns an error response if the guess does not succeed`() {
        val gameId = anyGameId()
        val guess = Code("Red", "Green", "Green", "Yellow")
        val app = MastermindApp(
            gameModule = GameModule(
                execute = { _ -> StreamNotFound("my-stream").left() }
            ),
            runnerModule = DirectRunnerModule()
        ).routes

        val response = app(Request(Method.POST, "/games/${gameId.value}/guesses").body(guess.pegs))

        response.status shouldBe Status.NOT_FOUND
    }
}

private fun Request.body(pegs: List<Code.Peg>): Request =
    Body.auto<List<String>>().toLens().invoke(pegs.map(Code.Peg::name), this)
