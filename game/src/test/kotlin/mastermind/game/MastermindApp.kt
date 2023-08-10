package mastermind.game

data class MastermindApp(
    val joinGame: suspend () -> GameId = { TODO() },
    val viewDecodingBoard: suspend (GameId) -> DecodingBoard? = { TODO() }
)
