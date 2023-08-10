package mastermind.game

interface MastermindApp {
    suspend fun joinGame(): GameId
    suspend fun viewDecodingBoard(gameId: GameId): DecodingBoard?
}