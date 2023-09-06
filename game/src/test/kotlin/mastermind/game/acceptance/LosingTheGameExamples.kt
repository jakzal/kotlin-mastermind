package mastermind.game.acceptance

import arrow.core.left
import mastermind.game.Code
import mastermind.game.Code.Peg.Companion.BLUE
import mastermind.game.Code.Peg.Companion.GREEN
import mastermind.game.Code.Peg.Companion.PURPLE
import mastermind.game.Code.Peg.Companion.RED
import mastermind.game.Code.Peg.Companion.YELLOW
import mastermind.game.GameError.GameFinishedError.GameAlreadyLost
import mastermind.game.acceptance.dsl.MastermindScenario
import mastermind.game.acceptance.dsl.ScenarioContext
import mastermind.game.acceptance.dsl.junit.ScenarioContextResolver
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
        // And the code maker placed the "Red Green Yellow Blue" secret on the board
        secret = Code(RED, GREEN, YELLOW, BLUE)
    ) {
        joinGame { gameId ->
            // When I try to break the code with an invalid pattern 12 times
            repeat(12) {
                makeGuess(gameId, Code(PURPLE, PURPLE, PURPLE, PURPLE))
            }
            // Then I should lose the game
            viewDecodingBoard(gameId) shouldBe DecodingBoard(
                gameId.value,
                secret.length,
                totalAttempts,
                (1..12).map { Guess(listOf("Purple", "Purple", "Purple", "Purple"), emptyList()) },
                "Lost"
            )
            // And I should no longer be able to make guesses
            makeGuess(gameId, Code(RED, GREEN, YELLOW, BLUE)) shouldReturn ExecutionFailure(
                GameAlreadyLost(gameId)
            ).left()
        }
    }
}