package mastermind.game

import java.util.*

@JvmInline
value class GameId(val value: String)

fun generateGameId(): GameId {
    return GameId(UUID.randomUUID().toString())
}
