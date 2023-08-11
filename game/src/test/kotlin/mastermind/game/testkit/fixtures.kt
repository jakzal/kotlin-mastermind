package mastermind.game.testkit

import mastermind.game.Code
import mastermind.game.GameId
import mastermind.game.generateGameId
import mastermind.game.makeCode

fun anySecret(): Code = makeCode()
fun anyGameId(): GameId = generateGameId()
