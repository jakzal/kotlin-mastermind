package mastermind.game

import arrow.core.Either
import mastermind.game.GameCommand.*
import mastermind.game.journal.InMemoryJournal
import mastermind.game.journal.Journal
import mastermind.game.journal.JournalCommandHandler
import mastermind.journal.JournalFailure
import mastermind.game.view.DecodingBoard
import mastermind.game.view.viewDecodingBoard

data class Configuration(
    val gameIdGenerator: GameIdGenerator = GameIdGenerator(::generateGameId),
    val codeMaker: CodeMaker = CodeMaker(::makeCode),
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
    private val configuration: Configuration = Configuration(),
    private val joinGameUseCase: suspend context(GameIdGenerator, CodeMaker, GameCommandHandler) () -> Either<JournalFailure<GameError>, GameId> = ::joinGame,
    private val makeGuessUseCase: suspend (MakeGuess) -> Either<JournalFailure<GameError>, GameId> = configuration.gameCommandHandler,
    private val decodingBoardQuery: suspend context(Journal<GameEvent>) (GameId) -> DecodingBoard? = ::viewDecodingBoard
) {
    suspend fun joinGame(): Either<JournalFailure<GameError>, GameId> = with(configuration) {
        joinGameUseCase(this, this, this)
    }

    suspend fun viewDecodingBoard(gameId: GameId): DecodingBoard? = with(configuration) {
        decodingBoardQuery(this, gameId)
    }

    suspend fun makeGuess(gameId: GameId, code: Code): Either<JournalFailure<GameError>, GameId> =
        makeGuessUseCase(MakeGuess(gameId, code))
}
