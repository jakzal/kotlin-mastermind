package mastermind.game

import arrow.core.Either
import mastermind.game.journal.*
import mastermind.game.view.DecodingBoard
import mastermind.game.view.viewDecodingBoard

data class Configuration(
    val gameIdGenerator: GameIdGenerator = GameIdGenerator { generateGameId() },
    val codeMaker: CodeMaker = CodeMaker { makeCode() },
    val journal: Journal<GameEvent> = InMemoryJournal(),
    val gameCommandHandler: GameCommandHandler = with(journal) {
        JournalCommandHandler(
            ::execute,
            { command -> "Mastermind:${command.gameId.value}" },
            { events -> events.head.gameId }
        )
    }
) : GameIdGenerator by gameIdGenerator,
    CodeMaker by codeMaker,
    GameCommandHandler by gameCommandHandler,
    Journal<GameEvent> by journal

data class MastermindApp(
    val configuration: Configuration = Configuration(),
    private val joinGame: JoinGameUseCase = JoinGameUseCase(::joinGame),
    private val viewDecodingBoard: DecodingBoardQuery = DecodingBoardQuery(::viewDecodingBoard)
) : JoinGameUseCase by joinGame,
    DecodingBoardQuery by viewDecodingBoard

fun interface JoinGameUseCase {
    context(GameIdGenerator, CodeMaker, GameCommandHandler)
    suspend fun joinGame(): Either<JournalFailure<GameFailure>, GameId>
}

fun interface DecodingBoardQuery {
    context(Journal<GameEvent>)
    suspend fun viewDecodingBoard(gameId: GameId): DecodingBoard?
}