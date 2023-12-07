package mastermind.game.acceptance.dsl

import arrow.core.Either
import mastermind.eventstore.EventStoreError
import mastermind.game.Code
import mastermind.game.GameError
import mastermind.game.GameId
import mastermind.game.view.DecodingBoard

interface PlayGameAbility {
    suspend fun joinGame(onceJoined: suspend PlayGameAbility.(GameId) -> Unit)
    suspend fun viewDecodingBoard(gameId: GameId): DecodingBoard?
    suspend fun makeGuess(gameId: GameId, code: Code): Either<EventStoreError<GameError>, GameId>
}