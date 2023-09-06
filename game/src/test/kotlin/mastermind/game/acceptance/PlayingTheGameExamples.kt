package mastermind.game.acceptance

import mastermind.game.Code
import mastermind.game.Code.Peg.Companion.BLUE
import mastermind.game.Code.Peg.Companion.GREEN
import mastermind.game.Code.Peg.Companion.PURPLE
import mastermind.game.Code.Peg.Companion.RED
import mastermind.game.Code.Peg.Companion.YELLOW
import mastermind.game.acceptance.dsl.MastermindScenario
import mastermind.game.acceptance.dsl.ScenarioContext
import mastermind.game.acceptance.dsl.junit.ScenarioContextResolver
import mastermind.game.view.DecodingBoard
import mastermind.game.view.Guess
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

context(ScenarioContext)
@ExtendWith(ScenarioContextResolver::class)
class PlayingTheGameExamples {
    @ParameterizedTest(name = "secret={0} guess={1} feedback={2}")
    @MethodSource("guessExamples")
    fun `code breaker gets feedback on their guess`(secret: Code, guess: Code, feedback: List<String>) =
        MastermindScenario(
            // Given a decoding board of 12 attempts
            totalAttempts = 12,
            // And the code maker placed the "Red Green Blue Yellow" code pattern on the board
            secret = secret
        ) {
            joinGame { gameId ->
                // When I try to break the code with "Red Purple Purple Purple"
                makeGuess(gameId, guess)
                // Then the code maker should give me "Black" feedback on my guess
                viewDecodingBoard(gameId) shouldReturnGuess Guess(guess.pegNames(), feedback)
            }
        }

    companion object {
        @JvmStatic
        fun guessExamples(): List<Arguments> = listOf(
            Arguments.of(
                Code(RED, GREEN, BLUE, YELLOW),
                Code(RED, PURPLE, PURPLE, PURPLE),
                listOf("Black")
            ),
            Arguments.of(
                Code(RED, GREEN, BLUE, YELLOW),
                Code(PURPLE, PURPLE, PURPLE, PURPLE),
                emptyList<String>()
            ),
            Arguments.of(
                Code(RED, GREEN, BLUE, YELLOW),
                Code(PURPLE, RED, PURPLE, PURPLE),
                listOf("White")
            ),
            Arguments.of(
                Code(RED, GREEN, BLUE, YELLOW),
                Code(RED, PURPLE, GREEN, PURPLE),
                listOf("Black", "White")
            ),
            Arguments.of(
                Code(RED, GREEN, BLUE, YELLOW),
                Code(RED, GREEN, BLUE, PURPLE),
                listOf("Black", "Black", "Black")
            ),
            Arguments.of(
                Code(RED, GREEN, BLUE, YELLOW),
                Code(RED, YELLOW, BLUE, GREEN),
                listOf("Black", "Black", "White", "White")
            ),
            Arguments.of(
                Code(RED, GREEN, BLUE, YELLOW),
                Code(YELLOW, BLUE, GREEN, RED),
                listOf("White", "White", "White", "White")
            ),
            Arguments.of(
                Code(RED, GREEN, BLUE, YELLOW),
                Code(RED, GREEN, BLUE, YELLOW),
                listOf("Black", "Black", "Black", "Black")
            ),
            Arguments.of(
                Code(RED, GREEN, BLUE, YELLOW),
                Code(RED, RED, RED, PURPLE),
                listOf("Black")
            ),
            Arguments.of(
                Code(GREEN, RED, BLUE, YELLOW),
                Code(GREEN, YELLOW, RED, BLUE),
                listOf("Black", "White", "White", "White")
            ),
            Arguments.of(
                Code(GREEN, RED, BLUE, YELLOW),
                Code(RED, GREEN, YELLOW, BLUE),
                listOf("White", "White", "White", "White")
            ),
            Arguments.of(
                Code(GREEN, RED, BLUE, YELLOW),
                Code(GREEN, RED, YELLOW, BLUE),
                listOf("Black", "Black", "White", "White")
            ),
            Arguments.of(
                Code(RED, GREEN, RED, YELLOW),
                Code(RED, RED, PURPLE, PURPLE),
                listOf("Black", "White")
            ),
            Arguments.of(
                Code(RED, RED, RED, YELLOW),
                Code(RED, GREEN, PURPLE, PURPLE),
                listOf("Black")
            ),
            Arguments.of(
                Code(RED, RED, BLUE, YELLOW),
                Code(PURPLE, PURPLE, RED, PURPLE),
                listOf("White")
            ),
            Arguments.of(
                Code(RED, BLUE, BLUE, YELLOW),
                Code(PURPLE, PURPLE, RED, RED),
                listOf("White")
            ),
        )
    }
}

private infix fun DecodingBoard?.shouldReturnGuess(guess: Guess) {
    assertNotNull(this, "Decoding board was found.")
    assertEquals(
        guess,
        this?.guesses?.last(),
        "`$guess` is the last found guess: `${this?.guesses?.joinToString(",") ?: ""}`."
    )
}

private fun Code.pegNames(): List<String> = pegs.map(Code.Peg::formattedName)
