package mastermind.game.view

import arrow.core.*
import kotlinx.coroutines.test.runTest
import mastermind.game.GameEvent
import mastermind.game.GameStarted
import mastermind.game.journal.Journal
import mastermind.game.journal.StreamName
import mastermind.game.journal.StreamNotFound
import mastermind.game.testkit.*
import org.junit.jupiter.api.Test

class DecodingBoardQueryExamples {
    @Test
    fun `it returns null if the game is not found`() = runTest {
        with(object : Journal<GameEvent> by fake() {
            override suspend fun load(streamName: StreamName) = StreamNotFound(streamName).left()
        }) {
            viewDecodingBoard(anyGameId()) shouldReturn null
        }
    }

    @Test
    fun `it returns the decoding board if the game is found`() = runTest {
        val gameId = anyGameId()
        val secret = anySecret()
        val totalAttempts = 12

        with(object : Journal<GameEvent> by fake() {
            override suspend fun load(streamName: StreamName): Either<Nothing, NonEmptyList<GameStarted>> {
                streamName shouldBe "Mastermind:${gameId.value}"
                return nonEmptyListOf(GameStarted(gameId, secret, totalAttempts)).right()
            }
        }) {
            viewDecodingBoard(gameId) shouldReturn DecodingBoard(
                gameId.value,
                secret.size,
                totalAttempts,
                emptyList(),
                "In progress"
            )
        }
    }
}