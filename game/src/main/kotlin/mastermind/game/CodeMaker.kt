package mastermind.game

import mastermind.game.Code.Peg.Companion.BLUE
import mastermind.game.Code.Peg.Companion.GREEN
import mastermind.game.Code.Peg.Companion.PURPLE
import mastermind.game.Code.Peg.Companion.RED
import mastermind.game.Code.Peg.Companion.YELLOW

fun makeCode(length: Int = 4): Code {
    return Code((1..length).map {
        listOf(RED, GREEN, BLUE, YELLOW, PURPLE).map(Code::Peg).random()
    })
}