package mastermind.game.view

import arrow.core.Either
import arrow.core.left
import arrow.core.nonEmptyListOf
import arrow.core.right
import kotlinx.coroutines.test.runTest
import mastermind.eventstore.EventStoreError
import mastermind.eventstore.EventStoreError.StreamNotFound
import mastermind.eventstore.Stream.LoadedStream
import mastermind.eventstore.StreamName
import mastermind.game.*
import mastermind.game.Feedback.Outcome
import mastermind.game.Feedback.Outcome.IN_PROGRESS
import mastermind.game.Feedback.Peg.BLACK
import mastermind.game.Feedback.Peg.WHITE
import mastermind.game.GameEvent.*
import mastermind.game.Guess
import mastermind.game.testkit.anyGameId
import mastermind.testkit.assertions.shouldBe
import mastermind.testkit.assertions.shouldReturn
import org.junit.jupiter.api.Test

class DecodingBoardQueryExamples {
    @Test
    fun `it returns null if the game is not found`() = runTest {
        viewDecodingBoard(noEvents(), anyGameId()) shouldReturn null
    }

    @Test
    fun `it returns the decoding board if the game is found`() = runTest {
        val gameId = anyGameId()
        val availablePegs = setOfPegs("Red", "Green", "Blue", "Yellow", "Purple", "Pink")
        val secret = availablePegs.makeCode()
        val totalAttempts = 12

        viewDecodingBoard(
            events(GameStarted(gameId, secret, totalAttempts, availablePegs)),
            gameId
        ) shouldReturn DecodingBoard(
            gameId.value,
            secret.length,
            totalAttempts,
            setOf("Red", "Green", "Blue", "Yellow", "Purple", "Pink"),
            emptyList(),
            "In progress"
        )
    }

    @Test
    fun `it builds the board from the history of events`() = runTest {
        val gameId = anyGameId()
        val availablePegs = setOfPegs("Red", "Green", "Blue", "Yellow", "Purple")
        val secret = availablePegs.makeCode()
        val totalAttempts = 12

        viewDecodingBoard(
            events(
                GameStarted(gameId, secret, totalAttempts, availablePegs),
                GuessMade(
                    gameId,
                    Guess(
                        Code("Red", "Green", "Blue", "Yellow"),
                        Feedback(IN_PROGRESS, BLACK, WHITE)
                    )
                )
            ), gameId
        ) shouldReturn DecodingBoard(
            gameId.value,
            secret.length,
            totalAttempts,
            setOf("Red", "Green", "Blue", "Yellow", "Purple"),
            listOf(
                Guess(listOf("Red", "Green", "Blue", "Yellow"), listOf("Black", "White"))
            ),
            "In progress"
        )
    }

    @Test
    fun `it builds the board for a lost game`() = runTest {
        val gameId = anyGameId()
        val secret = Code("Red", "Blue", "Yellow", "Yellow")
        val totalAttempts = 2
        val availablePegs = setOfPegs("Red", "Green", "Blue", "Yellow", "Purple")

        viewDecodingBoard(
            events(
                GameStarted(gameId, secret, totalAttempts, availablePegs),
                GuessMade(
                    gameId,
                    Guess(
                        Code("Red", "Green", "Blue", "Blue"),
                        Feedback(IN_PROGRESS, BLACK, WHITE)
                    )
                ),
                GuessMade(
                    gameId,
                    Guess(
                        Code("Purple", "Purple", "Purple", "Purple"),
                        Feedback(Outcome.LOST)
                    )
                ),
                GameLost(gameId)
            ), gameId
        ) shouldReturn DecodingBoard(
            gameId.value,
            secret.length,
            totalAttempts,
            setOf("Red", "Green", "Blue", "Yellow", "Purple"),
            listOf(
                Guess(listOf("Red", "Green", "Blue", "Blue"), listOf("Black", "White")),
                Guess(listOf("Purple", "Purple", "Purple", "Purple"), listOf())
            ),
            "Lost"
        )
    }

    @Test
    fun `it includes number of left attempts`() = runTest {
        val gameId = anyGameId()
        val secret = Code("Red", "Blue", "Yellow", "Yellow")
        val totalAttempts = 3
        val availablePegs = setOfPegs("Red", "Green", "Blue", "Yellow", "Purple")

        viewDecodingBoard(
            events(
                GameStarted(gameId, secret, totalAttempts, availablePegs),
                GuessMade(
                    gameId,
                    Guess(
                        Code("Red", "Green", "Blue", "Blue"),
                        Feedback(IN_PROGRESS, BLACK, WHITE)
                    )
                ),
                GuessMade(
                    gameId,
                    Guess(
                        Code("Purple", "Purple", "Purple", "Purple"),
                        Feedback(Outcome.LOST)
                    )
                ),
                GameLost(gameId)
            ), gameId
        ) shouldReturn DecodingBoard(
            gameId.value,
            secret.length,
            totalAttempts,
            setOf("Red", "Green", "Blue", "Yellow", "Purple"),
            listOf(
                Guess(listOf("Red", "Green", "Blue", "Blue"), listOf("Black", "White")),
                Guess(listOf("Purple", "Purple", "Purple", "Purple"), listOf())
            ),
            "Lost"
        )
    }


    @Test
    fun `it builds the board for a won game`() = runTest {
        val gameId = anyGameId()
        val availablePegs = setOfPegs("Red", "Green", "Blue", "Yellow", "Purple")
        val secret = availablePegs.makeCode()
        val totalAttempts = 12

        viewDecodingBoard(
            events(
                GameStarted(gameId, secret, totalAttempts, availablePegs),
                GuessMade(
                    gameId,
                    Guess(
                        Code("Red", "Green", "Blue", "Yellow"),
                        Feedback(IN_PROGRESS, BLACK, WHITE)
                    )
                ),
                GuessMade(
                    gameId,
                    Guess(
                        Code("Red", "Green", "Blue", "Yellow"),
                        Feedback(IN_PROGRESS, BLACK, WHITE)
                    )
                )
            ), gameId
        )?.leftAttempts shouldReturn 10
    }
}

private fun noEvents(): suspend (StreamName) -> Either<EventStoreError, LoadedStream<GameEvent>> =
    { streamName -> StreamNotFound(streamName).left() }

private fun events(
    event: GameEvent,
    vararg events: GameEvent
): suspend (StreamName) -> Either<EventStoreError, LoadedStream<GameEvent>> = { streamName ->
    streamName shouldBe "Mastermind:${event.gameId.value}"
    LoadedStream(streamName, events.size.toLong(), nonEmptyListOf(event, *events)).right()
}
