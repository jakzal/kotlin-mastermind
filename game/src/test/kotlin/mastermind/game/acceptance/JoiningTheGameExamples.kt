package mastermind.game.acceptance

import mastermind.game.Code
import mastermind.game.acceptance.dsl.MastermindScenario
import mastermind.game.acceptance.dsl.ScenarioContext
import mastermind.game.acceptance.dsl.junit.ScenarioContextResolver
import mastermind.game.setOfPegs
import mastermind.game.view.DecodingBoard
import mastermind.testkit.shouldReturn
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

context(ScenarioContext)
@ExtendWith(ScenarioContextResolver::class)
@Tag("http")
class JoiningTheGameExamples {
    @Test
    fun `code breaker joins the game`() = MastermindScenario(
        // Given a decoding board of 12 attempts
        totalAttempts = 12,
        // And the following code pegs available: "Red, Green, Blue, Yellow, Purple"
        availablePegs = setOfPegs("Red", "Green", "Blue", "Yellow", "Purple"),
        // And the code maker has placed a secret on the board
        secret = Code("Red", "Red", "Red", "Red")
    ) {
        // When I join the game
        joinGame { gameId ->
            // Then the game should be started with an empty board
            // And I should have 12 attempts available
            viewDecodingBoard(gameId) shouldReturn DecodingBoard(
                gameId.value,
                secret.length,
                totalAttempts,
                availablePegs.map(Code.Peg::name),
                emptyList(),
                "In progress"
            )
        }
    }
}