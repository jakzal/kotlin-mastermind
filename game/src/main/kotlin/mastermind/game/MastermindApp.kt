package mastermind.game

import arrow.core.Either
import mastermind.eventsourcing.Dispatch
import mastermind.eventsourcing.eventstore.EventSourcingError
import mastermind.eventsourcing.eventstore.EventStoreCommandDispatcher
import mastermind.eventsourcing.handlerFor
import mastermind.eventstore.EventStore
import mastermind.eventstore.InMemoryEventStore
import mastermind.game.GameCommand.JoinGame
import mastermind.game.GameCommand.MakeGuess
import mastermind.game.view.DecodingBoard

data class EventStoreModule<EVENT : Any>(
    private val eventStore: EventStore<EVENT> = InMemoryEventStore()
) : EventStore<EVENT> by eventStore

data class GameModule(
    val eventStoreModule: EventStoreModule<GameEvent> = EventStoreModule(),
    val availablePegs: Set<Code.Peg> = setOfPegs("Red", "Green", "Blue", "Yellow", "Purple"),
    val totalAttempts: Int = 12,
    val generateGameId: () -> GameId = ::generateGameId,
    val makeCode: () -> Code = { availablePegs.makeCode() },
    val viewDecodingBoard: suspend (GameId) -> DecodingBoard? = { gameId ->
        mastermind.game.view.viewDecodingBoard(eventStoreModule::load, gameId)
    },
    val execute: GameCommandDispatcher = EventStoreCommandDispatcher(
        eventStoreModule,
        handlerFor(::execute, ::applyEvent, ::notStartedGame),
        { command -> "Mastermind:${command.gameId.value}" },
        { events -> events.head.gameId }
    )
)

interface RunnerModule {
    fun start(app: MastermindApp)

    fun shutdown(app: MastermindApp)
}

data class MastermindApp(
    val gameModule: GameModule,
    val runnerModule: RunnerModule
) : AutoCloseable {
    constructor(
        eventStoreModule: EventStoreModule<GameEvent>,
        runnerModule: RunnerModule
    ) : this(GameModule(eventStoreModule), runnerModule)

    fun start() {
        runnerModule.start(this)
    }

    override fun close() {
        runnerModule.shutdown(this)
    }

    suspend fun joinGame(): Either<EventSourcingError<GameError>, GameId> = with(gameModule) {
        execute(JoinGame(generateGameId(), makeCode(), totalAttempts, availablePegs))
    }

    suspend fun makeGuess(command: MakeGuess): Either<EventSourcingError<GameError>, GameId> = with(gameModule) {
        execute(command)
    }

    suspend fun viewDecodingBoard(gameId: GameId): DecodingBoard? = with(gameModule) {
        viewDecodingBoard(gameId)
    }
}

typealias GameCommandDispatcher = Dispatch<GameCommand, EventSourcingError<GameError>, GameId>
