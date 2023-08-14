package mastermind.game.acceptance

import mastermind.game.acceptance.dsl.MastermindScenario
import mastermind.game.acceptance.dsl.ScenarioContext
import mastermind.game.acceptance.dsl.junit.ScenarioContextExtension
import mastermind.game.testkit.anySecret
import mastermind.game.testkit.shouldBe
import mastermind.game.view.DecodingBoard
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(ScenarioContextExtension::class)
@Tag("http")
class JoiningTheGameExamples {
    @Test
    fun `code breaker joins the game`(context: ScenarioContext) = MastermindScenario(
        context = context,
        // Given a decoding board of 12 attempts
        totalAttempts = 12,
        // And the code maker has placed a secret on the board
        secret = anySecret()
    ) {
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