package mastermind.game.view

data class DecodingBoard(
    val id: String,
    val size: Int,
    val totalAttempts: Int,
    val guesses: List<Guess>,
    val outcome: String
)

data class Guess(val code: List<String>, val feedback: List<String>)
