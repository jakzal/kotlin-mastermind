package mastermind.game

context(GameIdGenerator, CodeMaker, GameCommandHandler)
suspend fun joinGame(): GameId = handle(JoinGame(generateGameId(), makeCode(), 12))

fun interface CodeMaker {
    fun makeCode(): Code
}

fun interface GameIdGenerator {
    fun generateGameId(): GameId
}

fun interface GameCommandHandler {
    suspend fun handle(command: JoinGame): GameId
}
