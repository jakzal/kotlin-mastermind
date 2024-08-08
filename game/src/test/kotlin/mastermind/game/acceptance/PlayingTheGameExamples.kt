package mastermind.game.acceptance

import mastermind.game.Code
import mastermind.game.acceptance.dsl.mastermindScenario
import mastermind.game.acceptance.dsl.scenarios
import mastermind.game.setOfPegs
import mastermind.game.view.DecodingBoard
import mastermind.game.view.Guess
import mastermind.testkit.acceptance.junit.ExecutionContext
import mastermind.testkit.acceptance.junit.Scenario
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull

class PlayingTheGameExamples {
    @Scenario
    fun `code breaker gets feedback on their guess`(context: ExecutionContext) =
        scenarios(
            guessExamples { (secret, guess, feedback) ->
                "secret=${secret.pegNames()} guess=${guess.pegNames()} feedback=$feedback" to
                    context.mastermindScenario(
                        // Given a decoding board of 12 attempts
                        totalAttempts = 12,
                        // And the following code pegs available: "Red, Green, Blue, Yellow, Purple"
                        availablePegs = setOfPegs("Red", "Green", "Blue", "Yellow", "Purple"),
                        // And the code maker placed the "Red Green Blue Yellow" code pattern on the board
                        secret = secret
                    ) {
                        val alice = player("Alice")
                        alice.joinGame { gameId ->
                            // When I try to break the code with "Red Purple Purple Purple"
                            alice.makeGuess(gameId, guess)
                            // Then the code maker should give me "Black" feedback on my guess
                            alice.viewDecodingBoard(gameId) shouldReturnGuess Guess(guess.pegNames(), feedback)
                        }
                    }
            }
        )

    private fun <R> guessExamples(block: (Triple<Code, Code, List<String>>) -> R): List<R> = listOf(
        Triple(
            Code("Red", "Green", "Blue", "Yellow"),
            Code("Red", "Purple", "Purple", "Purple"),
            listOf("Black")
        ),
        Triple(
            Code("Red", "Green", "Blue", "Yellow"),
            Code("Purple", "Purple", "Purple", "Purple"),
            emptyList()
        ),
        Triple(
            Code("Red", "Green", "Blue", "Yellow"),
            Code("Purple", "Red", "Purple", "Purple"),
            listOf("White")
        ),
        Triple(
            Code("Red", "Green", "Blue", "Yellow"),
            Code("Red", "Purple", "Green", "Purple"),
            listOf("Black", "White")
        ),
        Triple(
            Code("Red", "Green", "Blue", "Yellow"),
            Code("Red", "Green", "Blue", "Purple"),
            listOf("Black", "Black", "Black")
        ),
        Triple(
            Code("Red", "Green", "Blue", "Yellow"),
            Code("Red", "Yellow", "Blue", "Green"),
            listOf("Black", "Black", "White", "White")
        ),
        Triple(
            Code("Red", "Green", "Blue", "Yellow"),
            Code("Yellow", "Blue", "Green", "Red"),
            listOf("White", "White", "White", "White")
        ),
        Triple(
            Code("Red", "Green", "Blue", "Yellow"),
            Code("Red", "Green", "Blue", "Yellow"),
            listOf("Black", "Black", "Black", "Black")
        ),
        Triple(
            Code("Red", "Green", "Blue", "Yellow"),
            Code("Red", "Red", "Red", "Purple"),
            listOf("Black")
        ),
        Triple(
            Code("Green", "Red", "Blue", "Yellow"),
            Code("Green", "Yellow", "Red", "Blue"),
            listOf("Black", "White", "White", "White")
        ),
        Triple(
            Code("Green", "Red", "Blue", "Yellow"),
            Code("Red", "Green", "Yellow", "Blue"),
            listOf("White", "White", "White", "White")
        ),
        Triple(
            Code("Green", "Red", "Blue", "Yellow"),
            Code("Green", "Red", "Yellow", "Blue"),
            listOf("Black", "Black", "White", "White")
        ),
        Triple(
            Code("Red", "Green", "Red", "Yellow"),
            Code("Red", "Red", "Purple", "Purple"),
            listOf("Black", "White")
        ),
        Triple(
            Code("Red", "Red", "Red", "Yellow"),
            Code("Red", "Green", "Purple", "Purple"),
            listOf("Black")
        ),
        Triple(
            Code("Red", "Red", "Blue", "Yellow"),
            Code("Purple", "Purple", "Red", "Purple"),
            listOf("White")
        ),
        Triple(
            Code("Red", "Blue", "Blue", "Yellow"),
            Code("Purple", "Purple", "Red", "Red"),
            listOf("White")
        ),
    ).map(block)
}

private infix fun DecodingBoard?.shouldReturnGuess(guess: Guess) {
    assertNotNull(this, "Decoding board was found.")
    assertEquals(
        guess,
        this?.guesses?.last(),
        "`$guess` is the last found guess: `${this?.guesses?.joinToString(",") ?: ""}`."
    )
}

private fun Code.pegNames(): List<String> = pegs.map(Code.Peg::name)
