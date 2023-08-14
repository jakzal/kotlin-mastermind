package mastermind.game

data class Code(val pegs: List<String>) : List<String> by pegs {
    constructor(vararg pegs: String) : this(pegs.toList())
}
