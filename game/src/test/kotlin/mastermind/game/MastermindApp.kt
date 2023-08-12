package mastermind.game

import arrow.core.Either
import mastermind.game.journal.InMemoryJournal
import mastermind.game.journal.Journal
import mastermind.game.journal.JournalCommandHandler
import mastermind.game.journal.JournalFailure
import mastermind.game.view.DecodingBoard
import mastermind.game.view.viewDecodingBoard

data class Configuration(
    val gameIdGenerator: GameIdGenerator = GameIdGenerator { generateGameId() },
    val codeMaker: CodeMaker = CodeMaker { makeCode() },
    val journalCommandHandler: JournalCommandHandler<GameCommand, GameEvent, GameFailure, GameId> = JournalCommandHandler(
        ::execute,
        { command -> "Mastermind:${command.gameId.value}" },
        { events -> events.head.gameId }
    ),
    val journal: Journal<GameEvent> = InMemoryJournal(),
    val gameCommandHandler: GameCommandHandler = GameCommandHandler { command ->
        with(journal) {
            journalCommandHandler(command)
        }
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