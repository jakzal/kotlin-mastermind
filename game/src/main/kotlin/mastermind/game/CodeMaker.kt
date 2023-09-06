package mastermind.game

fun makeCode(length: Int = 4): Code {
    return Code((1..length).map {
        Code.Peg.entries.random()
    })
}