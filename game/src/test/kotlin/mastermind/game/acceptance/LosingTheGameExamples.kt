package mastermind.game.acceptance

import arrow.core.left
import mastermind.game.Code
import mastermind.game.GameError.GameFinishedError.GameAlreadyLost
import mastermind.game.acceptance.dsl.ExecutionPlan
import mastermind.game.acceptance.dsl.junit.Scenario
import mastermind.game.acceptance.dsl.mastermindScenario
import mastermind.game.setOfPegs
import mastermind.game.view.DecodingBoard
import mastermind.game.view.Guess
import mastermind.journal.JournalError.ExecutionError
import mastermind.testkit.assertions.shouldReturn

context(ExecutionPlan)
class LosingTheGameExamples {
    @Scenario
    fun `code breaker loses the game`() = mastermindScenario(
        // Given a decoding board of 12 attempts
        totalAttempts = 12,
        // And the following code pegs available: "Red, Green, Blue, Yellow, Purple"
        availablePegs = setOfPegs("Red", "Green", "Blue", "Yellow", "Purple"),
        // And the code maker placed the "Red Green Yellow Blue" secret on the board
        secret = Code("Red", "Green", "Yellow", "Blue")
    ) {
        joinGame { gameId ->
            // When I try to break the code with an invalid pattern 12 times
            repeat(12) {
                makeGuess(gameId, Code("Purple", "Purple", "Purple", "Purple"))
            }
            // Then I should lose the game
            viewDecodingBoard(gameId) shouldReturn DecodingBoard(
                gameId.value,
                secret.length,
                totalAttempts,
                listOf("Red", "Green", "Blue", "Yellow", "Purple"),
                (1..12).map { Guess(listOf("Purple", "Purple", "Purple", "Purple"), emptyList()) },
                "Lost"
            )
            // And I should no longer be able to make guesses
            makeGuess(gameId, Code("Red", "Green", "Yellow", "Blue")) shouldReturn ExecutionError(
                GameAlreadyLost(gameId)
            ).left()
        }
    }
}