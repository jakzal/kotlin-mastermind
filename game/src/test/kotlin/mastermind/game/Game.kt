package mastermind.game

sealed interface GameCommand
data class JoinGame(val gameId: GameId, val secret: Code, val totalAttempts: Int) : GameCommand

sealed interface GameEvent
data class GameStarted(val gameId: GameId, val secret: Code, val totalAttempts: Int) : GameEvent
