package mastermind.game.http

import mastermind.game.GameId
import mastermind.game.GameModule
import mastermind.game.MastermindApp
import mastermind.game.testkit.DirectRunnerModule
import mastermind.game.view.DecodingBoard
import mastermind.testkit.assertions.shouldBe
import mastermind.testkit.assertions.shouldReturn
import org.http4k.core.*
import org.http4k.format.Jackson.auto
import org.junit.jupiter.api.Test

class ViewDecodingBoardHandlerExamples {
    @Test
    fun `it returns the decoding board view if found`() {
        val codePegs = setOf("Red", "Green", "Blue", "Yellow", "Purple")
        val app = MastermindApp(
            gameModule = GameModule(
                viewDecodingBoard = { gameId: GameId ->
                    DecodingBoard(gameId.value, 4, 8, codePegs, emptyList(), "In progress")
                }
            ),
            runnerModule = DirectRunnerModule()
        ).routes

        val response = app(Request(Method.GET, "/games/60693d0a-152c-4c4e-a11e-35fd8176df53"))

        response.status shouldBe Status.OK
        response.asDecodingBoard() shouldReturn DecodingBoard(
            "60693d0a-152c-4c4e-a11e-35fd8176df53",
            4,
            8,
            codePegs,
            emptyList(),
            "In progress"
        )
    }

    @Test
    fun `it returns a 404 response if the decoding board is not found`() {
        val app = MastermindApp(
            gameModule = GameModule(
                viewDecodingBoard = { null }
            ),
            runnerModule = DirectRunnerModule()
        ).routes

        val response = app(Request(Method.GET, "/games/60693d0a-152c-4c4e-a11e-35fd8176df53"))

        response.status shouldBe Status.NOT_FOUND
    }
}

private fun Response.asDecodingBoard(): DecodingBoard = Body.auto<DecodingBoard>().toLens()(this)
