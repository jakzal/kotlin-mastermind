package mastermind.game.http

import mastermind.game.GameError
import mastermind.game.GameError.GameFinishedError.GameAlreadyLost
import mastermind.game.GameError.GameFinishedError.GameAlreadyWon
import mastermind.game.GameError.GuessError.GameNotStarted
import mastermind.game.testkit.anyGameId
import mastermind.game.testkit.shouldReturn
import mastermind.journal.JournalFailure.EventStoreFailure.StreamNotFound
import mastermind.journal.JournalFailure.EventStoreFailure.VersionConflict
import mastermind.journal.JournalFailure.ExecutionFailure
import org.http4k.core.Response
import org.http4k.core.Status
import org.junit.jupiter.api.Test

class HandleFailureExamples {
    @Test
    fun `it returns a 404 response for StreamNotFound`() {
        StreamNotFound<GameError>("stream:a")
            .response() shouldReturn Response(Status.NOT_FOUND).with(Error("Game not found."))
    }

    @Test
    fun `it returns a 500 response for VersionConflict`() {
        VersionConflict<GameError>("stream:b")
            .response() shouldReturn Response(Status.INTERNAL_SERVER_ERROR).with(Error("Internal server error."))
    }

    @Test
    fun `it returns a 400 response for GameAlreadyWon`() {
        val gameId = anyGameId()
        ExecutionFailure<GameError>(GameAlreadyWon(gameId))
            .response() shouldReturn Response(Status.BAD_REQUEST).with(Error("Game `${gameId.value}` is already won."))
    }

    @Test
    fun `it returns a 400 response for GameAlreadyLost`() {
        val gameId = anyGameId()
        ExecutionFailure<GameError>(GameAlreadyLost(gameId))
            .response() shouldReturn Response(Status.BAD_REQUEST).with(Error("Game `${gameId.value}` is already lost."))
    }

    @Test
    fun `it returns a 400 response for GameNotStarted`() {
        val gameId = anyGameId()
        ExecutionFailure<GameError>(GameNotStarted(gameId))
            .response() shouldReturn Response(Status.BAD_REQUEST).with(Error("Game `${gameId.value}` not found."))
    }
}