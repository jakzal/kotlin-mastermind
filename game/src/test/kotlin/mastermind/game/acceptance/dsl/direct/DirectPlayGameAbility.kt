package mastermind.game.acceptance.dsl.direct

import arrow.core.getOrElse
import mastermind.game.Code
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

    override suspend fun makeGuess(code: Code) {
        app.makeGuess(code)
    }
}