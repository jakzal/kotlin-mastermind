package mastermind.game.view

import mastermind.game.GameError
import mastermind.game.GameEvent
import mastermind.game.GameEvent.*
import mastermind.game.GameId
import mastermind.journal.Journal

context(Journal<GameEvent, GameError>)
suspend fun viewDecodingBoard(gameId: GameId): DecodingBoard? = load("Mastermind:${gameId.value}")
    .fold(
        { null },
        { stream -> stream.events.fold(null, ::applyEventToDecodingBoard) }
    )

private fun applyEventToDecodingBoard(decodingBoard: DecodingBoard?, event: GameEvent): DecodingBoard? = when (event) {
    is GameStarted -> DecodingBoard(
        event.gameId.value,
        event.secret.size,
        event.totalAttempts,
        emptyList(),
        "In progress"
    )

    is GuessMade -> decodingBoard?.copy(
        guesses = decodingBoard.guesses + Guess(event.guess.code.pegs, event.guess.feedback.pegs)
    )

    is GameWon -> decodingBoard?.copy(outcome = "Won")
    is GameLost -> decodingBoard?.copy(outcome = "Lost")
}