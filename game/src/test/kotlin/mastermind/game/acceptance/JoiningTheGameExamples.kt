package mastermind.game.acceptance

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class JoiningTheGameExamples {
    @Test
    fun `code breaker joins the game`() {
        // Given a decoding board of 12 attempts
        val totalAttempts = 12
        // And the code maker has placed a secret on the board
        val secret = Code("Red", "Green", "Yellow", "Blue")
        startApplication(totalAttempts, secret)
        // When I join the game
        joinGame { gameId ->
            // Then the game should be started with an empty board
            // And I should have 12 attempts available
            viewDecodingBoard(gameId) shouldBe DecodingBoard(
                gameId.value,
                secret.size,
                totalAttempts,
                emptyList(),
                "In progress"
            )
        }
    }

}

data class Code(val pegs: List<String>) : List<String> by pegs {
    constructor(vararg pegs: String) : this(pegs.asList())
}

@JvmInline
value class GameId(val value: String)

data class DecodingBoard(
    val id: String,
    val size: Int,
    val totalAttempts: Int,
    val guesses: List<Any>,
    val outcome: String
) {

}

private fun startApplication(totalAttempts: Int, secret: Code) {
}

private fun joinGame(block: (GameId) -> Unit) {
}

private fun viewDecodingBoard(gameId: GameId): DecodingBoard? = DecodingBoard(
    gameId.value,
    4,
    12,
    emptyList(),
    "In progress"
)

infix fun <T> T?.shouldBe(expected: T?) {
    assertEquals(expected, this)
}
