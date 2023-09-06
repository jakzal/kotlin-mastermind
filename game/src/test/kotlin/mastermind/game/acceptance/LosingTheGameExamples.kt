package mastermind.game.acceptance

import arrow.core.left
import mastermind.game.Code
import mastermind.game.GameError.GameFinishedError.GameAlreadyLost
import mastermind.game.acceptance.dsl.MastermindScenario
import mastermind.game.acceptance.dsl.ScenarioContext
import mastermind.game.acceptance.dsl.junit.ScenarioContextResolver
import mastermind.game.setOfPegs
import mastermind.game.testkit.shouldBe
import mastermind.game.testkit.shouldReturn
import mastermind.game.view.DecodingBoard
import mastermind.game.view.Guess
import mastermind.journal.JournalFailure.ExecutionFailure
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

context(ScenarioContext)
@ExtendWith(ScenarioContextResolver::class)
class LosingTheGameExamples {
    @Test
    fun `code breaker loses the game`() = MastermindScenario(
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
            viewDecodingBoard(gameId) shouldBe DecodingBoard(
                gameId.value,
                secret.length,
                totalAttempts,
                listOf("Red", "Green", "Blue", "Yellow", "Purple"),
                (1..12).map { Guess(listOf("Purple", "Purple", "Purple", "Purple"), emptyList()) },
                "Lost"
            )
            // And I should no longer be able to make guesses
            makeGuess(gameId, Code("Red", "Green", "Yellow", "Blue")) shouldReturn ExecutionFailure(
                GameAlreadyLost(gameId)
            ).left()
        }
    }
}