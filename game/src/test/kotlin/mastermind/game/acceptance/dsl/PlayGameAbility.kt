package mastermind.game.acceptance.dsl

import mastermind.game.Code
import mastermind.game.GameId
import mastermind.game.view.DecodingBoard

interface PlayGameAbility {
    suspend fun joinGame(onceJoined: suspend PlayGameAbility.(GameId) -> Unit)
    suspend fun viewDecodingBoard(gameId: GameId): DecodingBoard?
    suspend fun makeGuess(code: Code)
}