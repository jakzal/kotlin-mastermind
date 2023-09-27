package mastermind.game.http

import mastermind.game.Code
import mastermind.game.GameError
import mastermind.game.GameError.GameFinishedError.GameAlreadyLost
import mastermind.game.GameError.GameFinishedError.GameAlreadyWon
import mastermind.game.GameError.GuessError.*
import mastermind.game.setOfPegs
import mastermind.game.testkit.anyGameId
import mastermind.journal.JournalError.StreamNotFound
import mastermind.journal.JournalError.VersionConflict
import mastermind.journal.JournalError.ExecutionError
import mastermind.testkit.assertions.shouldReturn
import org.http4k.core.Response
import org.http4k.core.Status
import org.junit.jupiter.api.Test

class HandleErrorExamples {
    @Test
    fun `it returns a 404 response for StreamNotFound`() {
        StreamNotFound<GameError>("stream:a")
            .response() shouldReturn Response(Status.NOT_FOUND).with(Error("Game not found."))
    }

    @Test
    fun `it returns a 500 response for VersionConflict`() {
        VersionConflict<GameError>("stream:b", 1L, 4L)
            .response() shouldReturn Response(Status.INTERNAL_SERVER_ERROR).with(Error("Internal server error."))
    }

    @Test
    fun `it returns a 400 response for GameAlreadyWon`() {
        val gameId = anyGameId()
        ExecutionError<GameError>(GameAlreadyWon(gameId))
            .response() shouldReturn Response(Status.BAD_REQUEST).with(Error("Game `${gameId.value}` is already won."))
    }

    @Test
    fun `it returns a 400 response for GameAlreadyLost`() {
        val gameId = anyGameId()
        ExecutionError<GameError>(GameAlreadyLost(gameId))
            .response() shouldReturn Response(Status.BAD_REQUEST).with(Error("Game `${gameId.value}` is already lost."))
    }

    @Test
    fun `it returns a 400 response for GameNotStarted`() {
        val gameId = anyGameId()
        ExecutionError<GameError>(GameNotStarted(gameId))
            .response() shouldReturn Response(Status.BAD_REQUEST).with(Error("Game `${gameId.value}` not found."))
    }

    @Test
    fun `it returns a 400 response for GuessTooShort`() {
        val gameId = anyGameId()
        ExecutionError<GameError>(GuessTooShort(gameId, Code("Red", "Green"), 4))
            .response() shouldReturn Response(Status.BAD_REQUEST).with(Error("Guess `Red, Green` is too short (required length is 4)."))
    }

    @Test
    fun `it returns a 400 response for GuessTooLong`() {
        val gameId = anyGameId()
        ExecutionError<GameError>(GuessTooLong(gameId, Code("Red", "Green", "Green", "Green", "Red"), 4))
            .response() shouldReturn Response(Status.BAD_REQUEST).with(Error("Guess `Red, Green, Green, Green, Red` is too long (required length is 4)."))
    }

    @Test
    fun `it returns a 400 response for InvalidPegInGuess`() {
        val gameId = anyGameId()
        ExecutionError<GameError>(InvalidPegInGuess(gameId, Code("Red", "Green"), setOfPegs("Green", "Yellow")))
            .response() shouldReturn Response(Status.BAD_REQUEST).with(Error("Guess `Red, Green` contains unrecognised pegs (available pegs are `Green, Yellow`)."))
    }
}