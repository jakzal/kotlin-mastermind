package mastermind.game

import arrow.core.Either
import mastermind.game.GameCommand.MakeGuess
import mastermind.game.journal.JournalCommandHandler
import mastermind.game.view.DecodingBoard
import mastermind.journal.InMemoryJournal
import mastermind.journal.Journal
import mastermind.journal.JournalFailure

data class Configuration(
    val gameIdGenerator: GameIdGenerator = GameIdGenerator(::generateGameId),
    val codeMaker: CodeMaker = CodeMaker(::makeCode),
    val journal: Journal<GameEvent, GameError> = InMemoryJournal(),
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
    Journal<GameEvent, GameError> by journal

data class MastermindApp(
    private val configuration: Configuration = Configuration(),
    private val joinGameUseCase: suspend () -> Either<JournalFailure<GameError>, GameId> = {
        with(configuration) {
            joinGame()
        }
    },
    private val makeGuessUseCase: suspend (MakeGuess) -> Either<JournalFailure<GameError>, GameId> = configuration.gameCommandHandler,
    private val decodingBoardQuery: suspend (GameId) -> DecodingBoard? = { gameId ->
        with(configuration) {
            mastermind.game.view.viewDecodingBoard(gameId)
        }
    }
) {
    suspend fun joinGame(): Either<JournalFailure<GameError>, GameId> = joinGameUseCase()

    suspend fun viewDecodingBoard(gameId: GameId): DecodingBoard? = decodingBoardQuery(gameId)

    suspend fun makeGuess(gameId: GameId, code: Code): Either<JournalFailure<GameError>, GameId> =
        makeGuessUseCase(MakeGuess(gameId, code))
}
