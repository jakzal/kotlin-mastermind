package mastermind.game

import java.util.*

fun generateGameId(): GameId {
    return GameId(UUID.randomUUID().toString())
}
