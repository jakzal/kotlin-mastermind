package mastermind.game.view

data class DecodingBoard(
    val id: String,
    val size: Int,
    val totalAttempts: Int,
    val guesses: List<Any>,
    val outcome: String
)