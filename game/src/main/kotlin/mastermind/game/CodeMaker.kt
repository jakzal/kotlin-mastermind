package mastermind.game

fun List<Code.Peg>.makeCode(length: Int = 4) = Code((1..length).map { this.random() })
