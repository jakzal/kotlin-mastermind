package mastermind.game.view

import mastermind.game.*
import mastermind.game.GameEvent.*
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
        event.secret.length,
        event.totalAttempts,
        emptyList(),
        "In progress"
    )

    is GuessMade -> decodingBoard?.copy(
        guesses = decodingBoard.guesses + Guess(event.guess.codePegs(), event.guess.feedbackPegs())
    )

    is GameWon -> decodingBoard?.copy(outcome = "Won")
    is GameLost -> decodingBoard?.copy(outcome = "Lost")
}

private fun mastermind.game.Guess.codePegs(): List<String> = code.pegs.map(Code.Peg::name)

private fun mastermind.game.Guess.feedbackPegs(): List<String> = feedback.pegs.map(Feedback.Peg::formattedName)
