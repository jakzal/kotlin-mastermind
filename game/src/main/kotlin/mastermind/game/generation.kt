package mastermind.game

import java.util.*

fun Set<Code.Peg>.makeCode(length: Int = 4) = Code((1..length).map { this.random() })

fun generateGameId(): GameId {
    return GameId(UUID.randomUUID().toString())
}
