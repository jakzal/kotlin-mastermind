package mastermind.game.acceptance.dsl.direct

import arrow.core.Either
import arrow.core.getOrElse
import mastermind.eventstore.EventStoreError
import mastermind.game.Code
import mastermind.game.GameCommand.MakeGuess
import mastermind.game.GameError
import mastermind.game.GameId
import mastermind.game.MastermindApp
import mastermind.game.acceptance.dsl.PlayGameAbility
import mastermind.game.view.DecodingBoard

class DirectPlayGameAbility(private val app: MastermindApp) : PlayGameAbility {
    override suspend fun joinGame(onceJoined: suspend PlayGameAbility.(GameId) -> Unit) {
        app.joinGame()
            .getOrElse {
                throw RuntimeException("JoinGame command completed with no error.")
            }
            .let {
                onceJoined(it)
            }
    }

    override suspend fun viewDecodingBoard(gameId: GameId): DecodingBoard? = app.viewDecodingBoard(gameId)

    override suspend fun makeGuess(gameId: GameId, code: Code): Either<EventStoreError<GameError>, GameId> =
        app.makeGuess(MakeGuess(gameId, code))
}