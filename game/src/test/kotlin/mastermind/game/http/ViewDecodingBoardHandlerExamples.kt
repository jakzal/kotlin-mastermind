package mastermind.game.http

import mastermind.game.view.DecodingBoard
import mastermind.game.GameId
import mastermind.game.MastermindApp
import mastermind.game.testkit.shouldBe
import org.http4k.core.*
import org.http4k.format.Jackson.auto
import org.junit.jupiter.api.Test

class ViewDecodingBoardHandlerExamples {
    @Test
    fun `it returns the decoding board view if found`() {
        val app = mastermindHttpApp(MastermindApp(
            decodingBoardQuery = { gameId: GameId ->
                DecodingBoard(gameId.value, 4, 8, emptyList(), "In progress")
            }
        ))

        val response = app(Request(Method.GET, "/games/60693d0a-152c-4c4e-a11e-35fd8176df53"))

        response.status shouldBe Status.OK
        response.asDecodingBoard() shouldBe DecodingBoard(
            "60693d0a-152c-4c4e-a11e-35fd8176df53",
            4,
            8,
            emptyList(),
            "In progress"
        )
    }

    @Test
    fun `it returns a 404 response if the decoding board is not found`() {
        val app = mastermindHttpApp(MastermindApp(
            decodingBoardQuery = { null }
        ))

        val response = app(Request(Method.GET, "/games/60693d0a-152c-4c4e-a11e-35fd8176df53"))

        response.status shouldBe Status.NOT_FOUND
    }
}

private fun Response.asDecodingBoard(): DecodingBoard = Body.auto<DecodingBoard>().toLens()(this)