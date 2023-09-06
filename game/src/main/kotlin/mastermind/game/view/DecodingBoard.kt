package mastermind.game.view

data class DecodingBoard(
    val id: String,
    val secretLength: Int,
    val totalAttempts: Int,
    val availablePegs: List<String>,
    val guesses: List<Guess>,
    val outcome: String
) {
    val leftAttempts: Int get() = totalAttempts - guesses.size
}

data class Guess(val code: List<String>, val feedback: List<String>)
