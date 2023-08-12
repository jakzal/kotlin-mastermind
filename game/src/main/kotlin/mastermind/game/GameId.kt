package mastermind.game

import java.util.*

data class GameId(val value: String)

fun generateGameId(): GameId {
    return GameId(UUID.randomUUID().toString())
}
