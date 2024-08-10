package mastermind.game.acceptance.dsl.direct

import arrow.core.Either
import arrow.core.getOrElse
import mastermind.eventsourcing.eventstore.EventSourcingError
import mastermind.game.Code
import mastermind.game.GameCommand.MakeGuess
import mastermind.game.GameError
import mastermind.game.GameId
import mastermind.game.MastermindApp
import mastermind.game.acceptance.dsl.GamePlayTasks
import mastermind.game.view.DecodingBoard

class DirectGamePlayTasks(private val app: MastermindApp) : GamePlayTasks {
    override suspend fun joinGame(onceJoined: suspend GamePlayTasks.(GameId) -> Unit) {
        app.joinGame()
            .getOrElse {
                throw RuntimeException("JoinGame command completed with no error.")
            }
            .let {
                onceJoined(it)
            }
    }

    override suspend fun viewDecodingBoard(gameId: GameId): DecodingBoard? = app.viewDecodingBoard(gameId)

    override suspend fun makeGuess(gameId: GameId, code: Code): Either<EventSourcingError<GameError>, GameId> =
        app.makeGuess(MakeGuess(gameId, code))
}