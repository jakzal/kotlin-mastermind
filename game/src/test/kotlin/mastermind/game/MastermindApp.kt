package mastermind.game

data class Configuration(
    val gameIdGenerator: GameIdGenerator = GameIdGenerator { generateGameId() },
    val codeMaker: CodeMaker = CodeMaker { makeCode() },
    val gameCommandHandler: GameCommandHandler = GameCommandHandler { TODO() }
) : GameIdGenerator by gameIdGenerator,
    CodeMaker by codeMaker,
    GameCommandHandler by gameCommandHandler

data class MastermindApp(
    val configuration: Configuration = Configuration(),
    val joinGame: suspend () -> GameId = { with(configuration) {
        mastermind.game.joinGame()
    }},
    val viewDecodingBoard: suspend (GameId) -> DecodingBoard? = { TODO() }
)
