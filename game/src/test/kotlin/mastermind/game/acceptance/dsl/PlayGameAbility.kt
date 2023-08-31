package mastermind.game.acceptance.dsl

import arrow.core.Either
import mastermind.game.Code
import mastermind.game.GameError
import mastermind.game.GameId
import mastermind.game.journal.JournalFailure
import mastermind.game.view.DecodingBoard

interface PlayGameAbility {
    suspend fun joinGame(onceJoined: suspend PlayGameAbility.(GameId) -> Unit)
    suspend fun viewDecodingBoard(gameId: GameId): DecodingBoard?
    suspend fun makeGuess(gameId: GameId, code: Code): Either<JournalFailure<GameError>, GameId>
}