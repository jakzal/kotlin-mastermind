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
    val updateStream: UpdateStream<EVENT, JournalError<ERROR>> = with(journal) { createUpdateStream() },
    val loadStream: LoadStream<EVENT, JournalError<ERROR>> = with(journal) { createLoadStream() }
) {
    companion object {
        fun <EVENT : Any, ERROR : Any> detect() = with(System.getenv("EVENTSTOREDB_URL")) {
            when(this) {
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

data class Configuration(
    val availablePegs: Set<Code.Peg> = setOfPegs("Red", "Green", "Blue", "Yellow", "Purple"),
    val totalAttempts: Int = 12,
    val generateGameId: () -> GameId = ::generateGameId,
    val makeCode: () -> Code = { availablePegs.makeCode() },
    val journalModule: JournalModule<GameEvent, GameError> = JournalModule.detect(),
    val gameCommandHandler: GameCommandHandler = with(journalModule.updateStream) {
        JournalCommandHandler(
            ::execute,
            { command -> "Mastermind:${command.gameId.value}" },
            { events -> events.head.gameId }
        )
    }
) : GameCommandHandler by gameCommandHandler

data class MastermindApp(
    private val configuration: Configuration = Configuration(),
    val joinGame: suspend () -> Either<JournalError<GameError>, GameId> = {
        with(configuration) {
            gameCommandHandler(JoinGame(generateGameId(), makeCode(), totalAttempts, availablePegs))
        }
    },
    val makeGuess: suspend (MakeGuess) -> Either<JournalError<GameError>, GameId> = { command ->
        with(configuration) {
            gameCommandHandler(command)
        }
    },
    val viewDecodingBoard: suspend (GameId) -> DecodingBoard? = { gameId ->
        with(configuration.journalModule.loadStream) {
            mastermind.game.view.viewDecodingBoard(gameId)
        }
    }
)

typealias GameCommandHandler = CommandHandler<GameCommand, JournalError<GameError>, GameId>
