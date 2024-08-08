package mastermind.game.acceptance

import mastermind.eventsourcing.eventstore.EventSourcingError.ExecutionError
import mastermind.game.Code
import mastermind.game.GameError.GameFinishedError.GameAlreadyLost
import mastermind.game.acceptance.dsl.mastermindScenario
import mastermind.game.setOfPegs
import mastermind.game.view.DecodingBoard
import mastermind.game.view.Guess
import mastermind.testkit.acceptance.junit.ExecutionContext
import mastermind.testkit.acceptance.junit.Scenario
import mastermind.testkit.assertions.shouldFailWith
import mastermind.testkit.assertions.shouldReturn

class LosingTheGameExamples {
    @Scenario
    fun `code breaker loses the game`(context: ExecutionContext) = context.mastermindScenario(
        // Given a decoding board of 12 attempts
        totalAttempts = 12,
        // And the following code pegs available: "Red, Green, Blue, Yellow, Purple"
        availablePegs = setOfPegs("Red", "Green", "Blue", "Yellow", "Purple"),
        // And the code maker placed the "Red Green Yellow Blue" secret on the board
        secret = Code("Red", "Green", "Yellow", "Blue")
    ) {
        val alice = player("Alice")
        alice.joinGame { gameId ->
            // When I try to break the code with an invalid pattern 12 times
            repeat(12) {
                alice.makeGuess(gameId, Code("Purple", "Purple", "Purple", "Purple"))
            }
            // Then I should lose the game
            alice.viewDecodingBoard(gameId) shouldReturn DecodingBoard(
                gameId.value,
                secret.length,
                totalAttempts,
                setOf("Red", "Green", "Blue", "Yellow", "Purple"),
                (1..12).map { Guess(listOf("Purple", "Purple", "Purple", "Purple"), emptyList()) },
                "Lost"
            )
            // And I should no longer be able to make guesses
            alice.makeGuess(gameId, Code("Red", "Green", "Yellow", "Blue")) shouldFailWith ExecutionError(GameAlreadyLost(gameId))
        }
    }
}