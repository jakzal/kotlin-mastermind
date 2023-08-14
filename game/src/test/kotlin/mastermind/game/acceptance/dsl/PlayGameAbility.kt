package mastermind.game.acceptance.dsl

import mastermind.game.GameId
import mastermind.game.view.DecodingBoard

interface PlayGameAbility {
    suspend fun joinGame(block: suspend PlayGameAbility.(GameId) -> Unit)
    suspend fun viewDecodingBoard(gameId: GameId): DecodingBoard
}