package mastermind.game

import arrow.core.Either
import mastermind.eventsourcing.CommandHandler
import mastermind.eventsourcing.journal.JournalCommandHandler
import mastermind.game.GameCommand.JoinGame
import mastermind.game.GameCommand.MakeGuess
import mastermind.game.view.DecodingBoard
import mastermind.journal.*

data class Configuration(
    val availablePegs: Set<Code.Peg> = setOfPegs("Red", "Green", "Blue", "Yellow", "Purple"),
    val totalAttempts: Int = 12,
    val generateGameId: () -> GameId = ::generateGameId,
    val makeCode: () -> Code = { availablePegs.makeCode() },
    val eventStore: EventStore<GameEvent, GameError> = InMemoryEventStore(),
    val updateStream: UpdateStream<GameEvent, GameError> = with(eventStore) { createUpdateStream() },
    val gameCommandHandler: GameCommandHandler = with(updateStream) {
        JournalCommandHandler(
            ::execute,
            { command -> "Mastermind:${command.gameId.value}" },
            { events -> events.head.gameId }
        )
    }
) : GameCommandHandler by gameCommandHandler

data class MastermindApp(
    private val configuration: Configuration = Configuration(),
    val joinGame: suspend () -> Either<JournalFailure<GameError>, GameId> = {
        with(configuration) {
            gameCommandHandler(JoinGame(generateGameId(), makeCode(), totalAttempts, availablePegs))
        }
    },
    val makeGuess: suspend (MakeGuess) -> Either<JournalFailure<GameError>, GameId> = { command ->
        with(configuration) {
            gameCommandHandler(command)
        }
    },
    val viewDecodingBoard: suspend (GameId) -> DecodingBoard? = { gameId ->
        with(configuration.eventStore::load) {
            mastermind.game.view.viewDecodingBoard(gameId)
        }
    }
)

typealias GameCommandHandler = CommandHandler<GameCommand, JournalFailure<GameError>, GameId>
