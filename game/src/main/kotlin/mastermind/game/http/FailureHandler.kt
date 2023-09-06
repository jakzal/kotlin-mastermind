package mastermind.game.http

import mastermind.game.GameError
import mastermind.game.GameError.GameFinishedError.GameAlreadyLost
import mastermind.game.GameError.GameFinishedError.GameAlreadyWon
import mastermind.game.GameError.GameNotStarted
import mastermind.journal.JournalFailure
import mastermind.journal.JournalFailure.EventStoreFailure
import mastermind.journal.JournalFailure.EventStoreFailure.StreamNotFound
import mastermind.journal.JournalFailure.EventStoreFailure.VersionConflict
import mastermind.journal.JournalFailure.ExecutionFailure
import org.http4k.core.Body
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.with
import org.http4k.format.Jackson.auto

data class Error(val message: String)

fun JournalFailure<GameError>.response(): Response = when (this) {
    is EventStoreFailure<GameError> -> when (this) {
        is StreamNotFound<GameError> -> Response(Status.NOT_FOUND).with(Error("Game not found."))
        is VersionConflict -> Response(Status.INTERNAL_SERVER_ERROR).with(Error("Internal server error."))
    }

    is ExecutionFailure<GameError> -> this.cause.response()
}

private fun GameError.response() =
    when (this) {
        is GameAlreadyWon -> Response(Status.BAD_REQUEST).with(Error("Game `${gameId.value}` is already won."))
        is GameAlreadyLost -> Response(Status.BAD_REQUEST).with(Error("Game `${gameId.value}` is already lost."))
        is GameNotStarted -> Response(Status.BAD_REQUEST).with(Error("Game `${gameId.value}` not found."))
    }

fun Response.with(error: Error): Response = with(Body.auto<Error>().toLens() of error)
