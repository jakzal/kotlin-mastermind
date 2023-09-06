package mastermind.game.view

import arrow.core.*
import kotlinx.coroutines.test.runTest
import mastermind.game.*
import mastermind.game.Code.Peg.*
import mastermind.game.Feedback.Peg.BLACK
import mastermind.game.Feedback.Peg.WHITE
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
        with(emptyJournal()) {
            viewDecodingBoard(anyGameId()) shouldReturn null
        }
    }

    @Test
    fun `it returns the decoding board if the game is found`() = runTest {
        val gameId = anyGameId()
        val secret = anySecret()
        val totalAttempts = 12

        with(journalOf(gameId, GameStarted(gameId, secret, totalAttempts))) {
            viewDecodingBoard(gameId) shouldReturn DecodingBoard(
                gameId.value,
                secret.length,
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

        with(
            journalOf(
                gameId,
                GameStarted(gameId, secret, totalAttempts),
                GuessMade(
                    gameId,
                    Guess(
                        Code(RED, GREEN, BLUE, YELLOW),
                        Feedback(listOf(BLACK, WHITE), Feedback.Outcome.IN_PROGRESS)
                    )
                )
            )
        ) {
            viewDecodingBoard(gameId) shouldReturn DecodingBoard(
                gameId.value,
                secret.length,
                totalAttempts,
                listOf(
                    Guess(listOf("Red", "Green", "Blue", "Yellow"), listOf("Black", "White"))
                ),
                "In progress"
            )
        }
    }

    @Test
    fun `it builds the board for a lost game`() = runTest {
        val gameId = anyGameId()
        val secret = Code(RED, BLUE, YELLOW, YELLOW)
        val totalAttempts = 2

        with(
            journalOf(
                gameId,
                GameStarted(gameId, secret, totalAttempts),
                GuessMade(
                    gameId,
                    Guess(
                        Code(RED, GREEN, BLUE, BLUE),
                        Feedback(listOf(BLACK, WHITE), Feedback.Outcome.IN_PROGRESS)
                    )
                ),
                GuessMade(
                    gameId,
                    Guess(
                        Code(PURPLE, PURPLE, PURPLE, PURPLE),
                        Feedback(listOf(), Feedback.Outcome.LOST)
                    )
                ),
                GameLost(gameId)
            )
        ) {
            viewDecodingBoard(gameId) shouldReturn DecodingBoard(
                gameId.value,
                secret.length,
                totalAttempts,
                listOf(
                    Guess(listOf("Red", "Green", "Blue", "Blue"), listOf("Black", "White")),
                    Guess(listOf("Purple", "Purple", "Purple", "Purple"), listOf())
                ),
                "Lost"
            )
        }
    }

    @Test
    fun `it includes number of left attempts`() = runTest {
        val gameId = anyGameId()
        val secret = Code(RED, BLUE, YELLOW, YELLOW)
        val totalAttempts = 3

        with(
            journalOf(
                gameId,
                GameStarted(gameId, secret, totalAttempts),
                GuessMade(
                    gameId,
                    Guess(
                        Code(RED, GREEN, BLUE, BLUE),
                        Feedback(listOf(BLACK, WHITE), Feedback.Outcome.IN_PROGRESS)
                    )
                ),
                GuessMade(
                    gameId,
                    Guess(
                        Code(PURPLE, PURPLE, PURPLE, PURPLE),
                        Feedback(listOf(), Feedback.Outcome.LOST)
                    )
                ),
                GameLost(gameId)
            )
        ) {
            viewDecodingBoard(gameId) shouldReturn DecodingBoard(
                gameId.value,
                secret.length,
                totalAttempts,
                listOf(
                    Guess(listOf("Red", "Green", "Blue", "Blue"), listOf("Black", "White")),
                    Guess(listOf("Purple", "Purple", "Purple", "Purple"), listOf())
                ),
                "Lost"
            )
        }
    }


    @Test
    fun `it builds the board for a won game`() = runTest {
        val gameId = anyGameId()
        val secret = anySecret()
        val totalAttempts = 12

        with(
            journalOf(
                gameId,
                GameStarted(gameId, secret, totalAttempts),
                GuessMade(
                    gameId,
                    Guess(
                        Code(RED, GREEN, BLUE, YELLOW),
                        Feedback(listOf(BLACK, WHITE), Feedback.Outcome.IN_PROGRESS)
                    )
                ),
                GuessMade(
                    gameId,
                    Guess(
                        Code(RED, GREEN, BLUE, YELLOW),
                        Feedback(listOf(BLACK, WHITE), Feedback.Outcome.IN_PROGRESS)
                    )
                )
            )
        ) {
            viewDecodingBoard(gameId)?.leftAttempts shouldReturn 10
        }
    }
}

private fun emptyJournal() = object : Journal<GameEvent, GameError> by fake() {
    override suspend fun load(streamName: StreamName): Either<EventStoreFailure<GameError>, LoadedStream<GameEvent>> =
        StreamNotFound<GameError>(streamName).left()
}

private fun journalOf(gameId: GameId, event: GameEvent, vararg events: GameEvent) =
    object : Journal<GameEvent, GameError> by fake() {
        override suspend fun load(streamName: StreamName): Either<EventStoreFailure<GameError>, LoadedStream<GameEvent>> {
            streamName shouldBe "Mastermind:${gameId.value}"
            return LoadedStream(
                streamName,
                events.size.toLong(),
                nonEmptyListOf(event, *events)
            ).right()
        }
    }
