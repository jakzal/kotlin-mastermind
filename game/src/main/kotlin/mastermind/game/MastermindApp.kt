package mastermind.game

import arrow.core.Either
import mastermind.eventsourcing.CommandHandler
import mastermind.eventsourcing.journal.JournalCommandHandler
import mastermind.eventsourcing.NoStateHandler
import mastermind.game.GameCommand.JoinGame
import mastermind.game.GameCommand.MakeGuess
import mastermind.game.view.DecodingBoard
import mastermind.journal.*

data class JournalModule<EVENT : Any, ERROR : Any>(
    private val journal: Journal<EVENT, ERROR> = InMemoryJournal(),
    private val updateStream: UpdateStream<EVENT, JournalError<ERROR>> = with(journal) { createUpdateStream() },
    private val loadStream: LoadStream<EVENT, JournalError<ERROR>> = with(journal) { createLoadStream() }
) : UpdateStream<EVENT, JournalError<ERROR>> by updateStream,
    LoadStream<EVENT, JournalError<ERROR>> by loadStream

data class GameModule(
    val journalModule: JournalModule<GameEvent, GameError> = JournalModule(),
    val availablePegs: Set<Code.Peg> = setOfPegs("Red", "Green", "Blue", "Yellow", "Purple"),
    val totalAttempts: Int = 12,
    val generateGameId: () -> GameId = ::generateGameId,
    val makeCode: () -> Code = { availablePegs.makeCode() },
    val viewDecodingBoard: suspend (GameId) -> DecodingBoard? = { gameId ->
        with(journalModule) {
            mastermind.game.view.viewDecodingBoard(gameId)
        }
    },
    val execute: GameCommandHandler = with(journalModule) {
        JournalCommandHandler(
            NoStateHandler(::execute),
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

typealias GameCommandHandler = CommandHandler<GameCommand, JournalError<GameError>, GameId>
