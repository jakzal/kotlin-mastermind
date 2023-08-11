package mastermind.game

sealed interface GameCommand {
    val gameId: GameId
}
data class JoinGame(override val gameId: GameId, val secret: Code, val totalAttempts: Int) : GameCommand

sealed interface GameEvent {
    val gameId: GameId
}
data class GameStarted(override val gameId: GameId, val secret: Code, val totalAttempts: Int) : GameEvent

sealed interface GameFailure
