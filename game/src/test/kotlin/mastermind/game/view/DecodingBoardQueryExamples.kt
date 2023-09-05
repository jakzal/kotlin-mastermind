package mastermind.game.view

import arrow.core.Either
import arrow.core.left
import arrow.core.nonEmptyListOf
import arrow.core.right
import kotlinx.coroutines.test.runTest
import mastermind.game.*
import mastermind.game.GameEvent.*
import mastermind.game.Guess
import mastermind.game.testkit.*
import mastermind.journal.Journal
import mastermind.journal.JournalFailure.EventStoreFailure
import mastermind.journal.JournalFailure.EventStoreFailure.StreamNotFound
import mastermind.journal.Stream.LoadedStream
import mastermind.journal.StreamName
import org.junit.jupiter.api.Test

class DecodingBoardQueryExamples {
    @Test
    fun `it returns null if the game is not found`() = runTest {
        with(object : Journal<GameEvent, GameError> by fake() {
            override suspend fun load(streamName: StreamName): Either<EventStoreFailure<GameError>, LoadedStream<GameEvent>> =
                StreamNotFound<GameError>(streamName).left()
        }) {
            viewDecodingBoard(anyGameId()) shouldReturn null
        }
    }

    @Test
    fun `it returns the decoding board if the game is found`() = runTest {
        val gameId = anyGameId()
        val secret = anySecret()
        val totalAttempts = 12

        with(object : Journal<GameEvent, GameError> by fake() {
            override suspend fun load(streamName: StreamName): Either<EventStoreFailure<GameError>, LoadedStream<GameEvent>> {
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

        with(object : Journal<GameEvent, GameError> by fake() {
            override suspend fun load(streamName: StreamName): Either<EventStoreFailure<GameError>, LoadedStream<GameEvent>> {
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

    @Test
    fun `it builds the board for a won game`() = runTest {
        val gameId = anyGameId()
        val secret = anySecret()
        val totalAttempts = 12

        with(object : Journal<GameEvent, GameError> by fake() {
            override suspend fun load(streamName: StreamName): Either<EventStoreFailure<GameError>, LoadedStream<GameEvent>> {
                return LoadedStream(
                    streamName,
                    4L,
                    nonEmptyListOf(
                        GameStarted(gameId, secret, totalAttempts),
                        GuessMade(
                            gameId,
                            Guess(
                                Code("Red", "Green", "Blue", "Yellow"),
                                Feedback(listOf("Black", "White"), Feedback.Outcome.IN_PROGRESS)
                            )
                        ),
                        GuessMade(
                            gameId,
                            Guess(
                                secret,
                                Feedback(listOf("Black", "Black", "Black", "Black"), Feedback.Outcome.WON)
                            )
                        ),
                        GameWon(gameId)
                    )
                ).right()
            }
        }) {
            viewDecodingBoard(gameId) shouldReturn DecodingBoard(
                gameId.value,
                secret.size,
                totalAttempts,
                listOf(
                    Guess(listOf("Red", "Green", "Blue", "Yellow"), listOf("Black", "White")),
                    Guess(secret.pegs, listOf("Black", "Black", "Black", "Black"))
                ),
                "Won"
            )
        }
    }

    @Test
    fun `it builds the board for a lost game`() = runTest {
        val gameId = anyGameId()
        val secret = Code("Red", "Blue", "Yellow", "Yellow")
        val totalAttempts = 2

        with(object : Journal<GameEvent, GameError> by fake() {
            override suspend fun load(streamName: StreamName): Either<EventStoreFailure<GameError>, LoadedStream<GameEvent>> {
                return LoadedStream(
                    streamName,
                    4L,
                    nonEmptyListOf(
                        GameStarted(gameId, secret, totalAttempts),
                        GuessMade(
                            gameId,
                            Guess(
                                Code("Red", "Green", "Blue", "Blue"),
                                Feedback(listOf("Black", "White"), Feedback.Outcome.IN_PROGRESS)
                            )
                        ),
                        GuessMade(
                            gameId,
                            Guess(
                                Code("Purple", "Purple", "Purple", "Purple"),
                                Feedback(listOf(), Feedback.Outcome.LOST)
                            )
                        ),
                        GameLost(gameId)
                    )
                ).right()
            }
        }) {
            viewDecodingBoard(gameId) shouldReturn DecodingBoard(
                gameId.value,
                secret.size,
                totalAttempts,
                listOf(
                    Guess(listOf("Red", "Green", "Blue", "Blue"), listOf("Black", "White")),
                    Guess(listOf("Purple", "Purple", "Purple", "Purple"), listOf())
                ),
                "Lost"
            )
        }
    }
}