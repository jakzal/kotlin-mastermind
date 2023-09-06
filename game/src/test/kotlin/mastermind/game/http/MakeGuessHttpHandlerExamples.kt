package mastermind.game.http

import arrow.core.left
import arrow.core.right
import mastermind.game.Code
import mastermind.game.GameCommand.MakeGuess
import mastermind.game.GameError
import mastermind.game.GameError.GameFinishedError.GameAlreadyLost
import mastermind.game.GameError.GameFinishedError.GameAlreadyWon
import mastermind.game.MastermindApp
import mastermind.game.testkit.anyGameId
import mastermind.game.testkit.shouldBe
import mastermind.game.testkit.shouldReturn
import mastermind.journal.JournalFailure.EventStoreFailure.StreamNotFound
import mastermind.journal.JournalFailure.ExecutionFailure
import org.http4k.core.*
import org.http4k.format.Jackson.auto
import org.junit.jupiter.api.Test

class MakeGuessHttpHandlerExamples {
    @Test
    fun `it returns the location of the game after making a guess`() {
        val gameId = anyGameId()
        val guess = Code("Red", "Green", "Green", "Yellow")
        val app = mastermindHttpApp(MastermindApp(
            makeGuess = { command: MakeGuess ->
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
            makeGuess = { _: MakeGuess ->
                StreamNotFound<GameError>("my-stream").left()
            }
        ))

        val response = app(Request(Method.POST, "/games/${gameId.value}/guesses").body(guess.pegs))

        response.status shouldBe Status.NOT_FOUND
    }

    @Test
    fun `it returns a 400 error in case game is already won`() {
        val gameId = anyGameId()
        val guess = Code("Red", "Green", "Green", "Yellow")
        val app = mastermindHttpApp(MastermindApp(
            makeGuess = { _: MakeGuess ->
                ExecutionFailure<GameError>(GameAlreadyWon(gameId)).left()
            }
        ))

        val response = app(Request(Method.POST, "/games/${gameId.value}/guesses").body(guess.pegs))

        response.status shouldBe Status.BAD_REQUEST
        response.body() shouldReturn Error("Game `${gameId.value}` is already won.")
    }

    @Test
    fun `it returns a 400 error in case game is already lost`() {
        val gameId = anyGameId()
        val guess = Code("Red", "Green", "Green", "Yellow")
        val app = mastermindHttpApp(MastermindApp(
            makeGuess = { _: MakeGuess ->
                ExecutionFailure<GameError>(GameAlreadyLost(gameId)).left()
            }
        ))

        val response = app(Request(Method.POST, "/games/${gameId.value}/guesses").body(guess.pegs))

        response.status shouldBe Status.BAD_REQUEST
        response.body() shouldReturn Error("Game `${gameId.value}` is already lost.")
    }
}

private fun Request.body(pegs: List<String>): Request = Body.auto<List<String>>().toLens().invoke(pegs, this)
private fun Response.body(): Error = Body.auto<Error>().toLens().extract(this)
