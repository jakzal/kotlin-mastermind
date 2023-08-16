package mastermind.game.view

import arrow.core.Either
import arrow.core.left
import arrow.core.nonEmptyListOf
import arrow.core.right
import kotlinx.coroutines.test.runTest
import mastermind.game.*
import mastermind.game.Guess
import mastermind.game.journal.EventStoreFailure
import mastermind.game.journal.Journal
import mastermind.game.journal.Stream.LoadedStream
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
            override suspend fun load(streamName: StreamName): Either<EventStoreFailure, LoadedStream<GameEvent>> {
                streamName shouldBe "Mastermind:${gameId.value}"
                return LoadedStream(
                    streamName,
                    1L,
                    nonEmptyListOf<GameEvent>(GameStarted(gameId, secret, totalAttempts))
                ).right()
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

    @Test
    fun `it builds the board from the history of events`() = runTest {
        val gameId = anyGameId()
        val secret = anySecret()
        val totalAttempts = 12

        with(object : Journal<GameEvent> by fake() {
            override suspend fun load(streamName: StreamName): Either<Nothing, LoadedStream<GameEvent>> {
                streamName shouldBe "Mastermind:${gameId.value}"
                return LoadedStream(
                    streamName,
                    2L,
                    nonEmptyListOf(
                        GameStarted(gameId, secret, totalAttempts),
                        GuessMade(
                            gameId,
                            Guess(
                                Code("Red", "Green", "Blue", "Yellow"),
                                Feedback(listOf("Black", "White"), Feedback.Outcome.IN_PROGRESS)
                            )
                        )
                    )
                ).right()
            }
        }) {
            viewDecodingBoard(gameId) shouldReturn DecodingBoard(
                gameId.value,
                secret.size,
                totalAttempts,
                listOf(
                    Guess(listOf("Red", "Green", "Blue", "Yellow"), listOf("Black", "White"))
                ),
                "In progress"
            )
        }
    }
}