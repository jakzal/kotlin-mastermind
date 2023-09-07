package mastermind.game.acceptance

import arrow.core.left
import mastermind.game.Code
import mastermind.game.GameError.GameFinishedError.GameAlreadyWon
import mastermind.game.acceptance.dsl.MastermindScenario
import mastermind.game.acceptance.dsl.ScenarioContext
import mastermind.game.acceptance.dsl.junit.ScenarioContextResolver
import mastermind.game.setOfPegs
import mastermind.game.view.DecodingBoard
import mastermind.game.view.Guess
import mastermind.journal.JournalFailure.ExecutionFailure
import mastermind.testkit.shouldReturn
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

context(ScenarioContext)
@ExtendWith(ScenarioContextResolver::class)
class WinningTheGameExamples {
    @Test
    fun `code breaker wins the game`() = MastermindScenario(
        // Given a decoding board of 12 attempts
        totalAttempts = 12,
        // And the following code pegs available: "Red, Green, Blue, Yellow, Purple"
        availablePegs = setOfPegs("Red", "Green", "Blue", "Yellow", "Purple"),
        // And the code maker placed the "Red Green Yellow Blue" secret on the board
        secret = Code("Red", "Green", "Yellow", "Blue")
    ) {
        joinGame { gameId ->
            // When I try to break the code with an invalid pattern 11 times
            repeat(11) {
                makeGuess(gameId, Code("Purple", "Purple", "Purple", "Purple"))
            }
            // But I break the code in the final guess
            makeGuess(gameId, Code("Red", "Green", "Yellow", "Blue"))
            // Then I should win the game
            viewDecodingBoard(gameId) shouldReturn DecodingBoard(
                gameId.value,
                secret.length,
                totalAttempts,
                listOf("Red", "Green", "Blue", "Yellow", "Purple"),
                (1..11).map { Guess(listOf("Purple", "Purple", "Purple", "Purple"), emptyList()) } + Guess(
                    listOf(
                        "Red",
                        "Green",
                        "Yellow",
                        "Blue"
                    ), listOf("Black", "Black", "Black", "Black")
                ),
                "Won"
            )
            // And I should no longer be able to make guesses
            makeGuess(gameId, Code("Red", "Green", "Yellow", "Blue")) shouldReturn ExecutionFailure(
                GameAlreadyWon(gameId)
            ).left()
        }
    }
}