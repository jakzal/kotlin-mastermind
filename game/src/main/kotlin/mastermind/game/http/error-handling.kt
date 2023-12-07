package mastermind.game.http

import mastermind.eventstore.EventStoreError
import mastermind.eventstore.EventStoreError.*
import mastermind.game.Code
import mastermind.game.GameError
import mastermind.game.GameError.GameFinishedError.GameAlreadyLost
import mastermind.game.GameError.GameFinishedError.GameAlreadyWon
import mastermind.game.GameError.GuessError.*
import org.http4k.core.Body
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.with
import org.http4k.format.Jackson.auto

data class Error(val message: String)

fun EventStoreError<GameError>.response(): Response = when (this) {
    is StreamNotFound -> Response(Status.NOT_FOUND).with(Error("Game not found."))
    is VersionConflict -> Response(Status.INTERNAL_SERVER_ERROR).with(Error("Internal server error."))
    is ExecutionError<GameError> -> this.cause.response()
}

private fun GameError.response() =
    when (this) {
        is GameAlreadyWon -> Response(Status.BAD_REQUEST).with(Error("Game `${gameId.value}` is already won."))
        is GameAlreadyLost -> Response(Status.BAD_REQUEST).with(Error("Game `${gameId.value}` is already lost."))
        is GameNotStarted -> Response(Status.BAD_REQUEST).with(Error("Game `${gameId.value}` not found."))
        is GuessTooLong -> Response(Status.BAD_REQUEST).with(Error("Guess `${guess.pegs.formattedForResponse()}` is too long (required length is ${requiredLength})."))
        is GuessTooShort -> Response(Status.BAD_REQUEST).with(Error("Guess `${guess.pegs.formattedForResponse()}` is too short (required length is ${requiredLength})."))
        is InvalidPegInGuess -> Response(Status.BAD_REQUEST).with(Error("Guess `${guess.pegs.formattedForResponse()}` contains unrecognised pegs (available pegs are `${availablePegs.formattedForResponse()}`)."))
    }

private fun Collection<Code.Peg>.formattedForResponse(): String = joinToString(", ", transform = Code.Peg::name)

fun Response.with(error: Error): Response = with(Body.auto<Error>().toLens() of error)
