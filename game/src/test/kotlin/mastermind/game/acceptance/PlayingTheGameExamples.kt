package mastermind.game.acceptance

import mastermind.game.Code
import mastermind.game.acceptance.dsl.MastermindScenario
import mastermind.game.acceptance.dsl.ScenarioContext
import mastermind.game.acceptance.dsl.junit.ScenarioContextResolver
import mastermind.game.view.DecodingBoard
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

context(ScenarioContext)
@ExtendWith(ScenarioContextResolver::class)
class PlayingTheGameExamples {
    @Test
    @Disabled
    fun `code breaker gets feedback on their guess`() = MastermindScenario(
        // Given a decoding board of 12 attempts
        totalAttempts = 12,
        // And the code maker placed the "Red Green Blue Yellow" code pattern on the board
        secret = Code("Red", "Green", "Blue", "Yellow")
    ) {
        joinGame { gameId ->
            // When I try to break the code with "Red Purple Purple Purple"
            makeGuess(Code("Red", "Purple", "Purple", "Purple"))
            // Then the code maker should give me "Black" feedback on my guess
            viewDecodingBoard(gameId) shouldReturnGuess listOf("Black")
        }
    }
}

private infix fun DecodingBoard?.shouldReturnGuess(guess: List<String>) {
    assertNotNull(this, "Decoding board was found.")
    assertTrue(
        this?.guesses?.contains(guess) ?: false,
        "Guess `$guess` is found in recent guesses: `${this?.guesses?.joinToString(",") ?: ""}`."
    )
}
