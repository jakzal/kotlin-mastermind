package mastermind.game

import arrow.core.Either
import mastermind.eventsourcing.Dispatch
import mastermind.eventsourcing.handlerFor
import mastermind.eventsourcing.journal.JournalCommandDispatcher
import mastermind.game.GameCommand.JoinGame
import mastermind.game.GameCommand.MakeGuess
import mastermind.game.view.DecodingBoard
import mastermind.journal.InMemoryJournalCatalogue
import mastermind.journal.JournalCatalogue
import mastermind.journal.JournalError

data class JournalModule<ENTRY : Any, ERROR : Any>(
    private val journalCatalogue: JournalCatalogue<ENTRY, ERROR> = InMemoryJournalCatalogue()
) : JournalCatalogue<ENTRY, ERROR> by journalCatalogue

data class GameModule(
    val journalModule: JournalModule<GameEvent, GameError> = JournalModule(),
    val availablePegs: Set<Code.Peg> = setOfPegs("Red", "Green", "Blue", "Yellow", "Purple"),
    val totalAttempts: Int = 12,
    val generateGameId: () -> GameId = ::generateGameId,
    val makeCode: () -> Code = { availablePegs.makeCode() },
    val viewDecodingBoard: suspend (GameId) -> DecodingBoard? = { gameId ->
        with(journalModule::load) {
            mastermind.game.view.viewDecodingBoard(gameId)
        }
    },
    val execute: GameCommandDispatcher = with(journalModule) {
        JournalCommandDispatcher(
            handlerFor(::execute, ::applyEvent, ::notStartedGame),
            { command -> "Mastermind:${command.gameId.value}" },
            { events -> events.head.gameId }
        )
    }
)

interface RunnerModule {
    context(MastermindApp)
    fun start()

    context(MastermindApp)
    fun shutdown()
}

data class MastermindApp(
    val gameModule: GameModule,
    val runnerModule: RunnerModule
) : AutoCloseable {
    constructor(
        journalModule: JournalModule<GameEvent, GameError>,
        runnerModule: RunnerModule
    ) : this(GameModule(journalModule), runnerModule)

    fun start() {
        runnerModule.start()
    }

    override fun close() {
        runnerModule.shutdown()
    }

    suspend fun joinGame(): Either<JournalError<GameError>, GameId> = with(gameModule) {
        execute(JoinGame(generateGameId(), makeCode(), totalAttempts, availablePegs))
    }

    suspend fun makeGuess(command: MakeGuess): Either<JournalError<GameError>, GameId> = with(gameModule) {
        execute(command)
    }

    suspend fun viewDecodingBoard(gameId: GameId): DecodingBoard? = with(gameModule) {
        viewDecodingBoard(gameId)
    }
}

typealias GameCommandDispatcher = Dispatch<GameCommand, JournalError<GameError>, GameId>
