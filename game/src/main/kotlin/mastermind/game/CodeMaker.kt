package mastermind.game

fun makeCode(length: Int = 4): Code {
    return Code((1..length).map {
        listOf("Red", "Green", "Blue", "Yellow", "Purple").map(Code::Peg).random()
    })
}