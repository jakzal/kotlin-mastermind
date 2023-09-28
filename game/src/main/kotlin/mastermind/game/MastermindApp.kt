package mastermind.game

import arrow.core.Either
import mastermind.eventsourcing.CommandHandler
import mastermind.eventsourcing.journal.JournalCommandHandler
import mastermind.game.GameCommand.JoinGame
import mastermind.game.GameCommand.MakeGuess
import mastermind.game.view.DecodingBoard
import mastermind.journal.*
import mastermind.journal.eventstoredb.EventStoreDbJournal

data class JournalModule<EVENT : Any, ERROR : Any>(
    private val journal: Journal<EVENT, ERROR>,
    private val updateStream: UpdateStream<EVENT, JournalError<ERROR>> = with(journal) { createUpdateStream() },
    private val loadStream: LoadStream<EVENT, JournalError<ERROR>> = with(journal) { createLoadStream() }
) : UpdateStream<EVENT, JournalError<ERROR>> by updateStream,
    LoadStream<EVENT, JournalError<ERROR>> by loadStream {
    companion object {
        fun <EVENT : Any, ERROR : Any> detect() = with(System.getenv("EVENTSTOREDB_URL")) {
            when (this) {
                null -> inMemory<EVENT, ERROR>()
                else -> eventStoreDb(this)
            }
        }

        private fun <EVENT : Any, ERROR : Any> inMemory() =
            JournalModule(InMemoryJournal<EVENT, ERROR>())

        private fun <EVENT : Any, ERROR : Any> eventStoreDb(connectionString: String) =
            JournalModule(EventStoreDbJournal<EVENT, ERROR>(connectionString))
    }
}

data class GameModule(
    val journalModule: JournalModule<GameEvent, GameError> = JournalModule.detect(),
    val availablePegs: Set<Code.Peg> = setOfPegs("Red", "Green", "Blue", "Yellow", "Purple"),
    val totalAttempts: Int = 12,
    val generateGameId: () -> GameId = ::generateGameId,
    val makeCode: () -> Code = { availablePegs.makeCode() },
    val viewDecodingBoard: suspend (GameId) -> DecodingBoard? = { gameId ->
        with(journalModule) {
            mastermind.game.view.viewDecodingBoard(gameId)
        }
    },
    val gameCommandHandler: GameCommandHandler = with(journalModule) {
        JournalCommandHandler(
            ::execute,
            { command -> "Mastermind:${command.gameId.value}" },
            { events -> events.head.gameId }
        )
    }
)

data class MastermindApp(
    private val journalModule: JournalModule<GameEvent, GameError> = JournalModule.detect(),
    private val gameModule: GameModule = GameModule(journalModule = journalModule)
) {
    suspend fun joinGame(): Either<JournalError<GameError>, GameId> = with(gameModule) {
        gameCommandHandler(JoinGame(generateGameId(), makeCode(), totalAttempts, availablePegs))
    }

    suspend fun makeGuess(command: MakeGuess): Either<JournalError<GameError>, GameId> = with(gameModule) {
        gameCommandHandler(command)
    }

    suspend fun viewDecodingBoard(gameId: GameId): DecodingBoard? = with(gameModule) {
        viewDecodingBoard(gameId)
    }
}

typealias GameCommandHandler = CommandHandler<GameCommand, JournalError<GameError>, GameId>
