package mastermind.game

import arrow.core.getOrElse
import mastermind.game.Feedback.Peg.BLACK
import mastermind.game.Feedback.Peg.WHITE
import mastermind.game.GameCommand.JoinGame
import mastermind.game.GameCommand.MakeGuess
import mastermind.game.GameError.GameFinishedError.GameAlreadyLost
import mastermind.game.GameError.GameFinishedError.GameAlreadyWon
import mastermind.game.GameError.GuessError.*
import mastermind.game.GameEvent.*
import mastermind.game.testkit.anyGameId
import mastermind.testkit.assertions.shouldFailWith
import mastermind.testkit.assertions.shouldSucceedWith
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

class GameExamples {
    private val gameId = anyGameId()
    private val secret = Code("Red", "Green", "Blue", "Yellow")
    private val totalAttempts = 12
    private val availablePegs = setOfPegs("Red", "Green", "Blue", "Yellow", "Purple", "Pink")

    @Test
    fun `it executes the JoinGame command`() {
        execute(JoinGame(gameId, secret, totalAttempts, availablePegs)) shouldSucceedWith listOf(
            GameStarted(
                gameId,
                secret,
                totalAttempts,
                availablePegs
            )
        )
    }

    @Test
    fun `it executes the MakeGuess command`() {
        val game = listOf(GameStarted(gameId, secret, totalAttempts, availablePegs))

        execute(MakeGuess(gameId, Code("Purple", "Purple", "Purple", "Purple")), game) shouldSucceedWith listOf(
            GuessMade(
                gameId,
                Guess(
                    Code("Purple", "Purple", "Purple", "Purple"),
                    Feedback(emptyList(), Feedback.Outcome.IN_PROGRESS)
                )
            )
        )
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("guessExamples")
    fun `it gives feedback on the guess`(message: String, secret: Code, guess: Code, feedback: Feedback) {
        val game = listOf(GameStarted(gameId, secret, totalAttempts, availablePegs))

        execute(MakeGuess(gameId, guess), game) shouldSucceedWith listOf(GuessMade(gameId, Guess(guess, feedback)))
    }

    companion object {
        @JvmStatic
        fun guessExamples(): List<Arguments> = listOf(
            Arguments.of(
                "it gives a black peg for each code peg on the correct position",
                Code("Red", "Green", "Blue", "Yellow"),
                Code("Red", "Purple", "Blue", "Purple"),
                Feedback(listOf(BLACK, BLACK), Feedback.Outcome.IN_PROGRESS)
            ),
            Arguments.of(
                "it gives no black peg for code peg duplicated on a wrong position",
                Code("Red", "Green", "Blue", "Yellow"),
                Code("Red", "Red", "Purple", "Purple"),
                Feedback(listOf(BLACK), Feedback.Outcome.IN_PROGRESS)
            ),
            Arguments.of(
                "it gives a white peg for code peg that is part of the code but is placed on a wrong position",
                Code("Red", "Green", "Blue", "Yellow"),
                Code("Purple", "Red", "Purple", "Purple"),
                Feedback(listOf(WHITE), Feedback.Outcome.IN_PROGRESS)
            ),
            Arguments.of(
                "it gives no white peg for code peg duplicated on a wrong position",
                Code("Red", "Green", "Blue", "Yellow"),
                Code("Purple", "Red", "Red", "Purple"),
                Feedback(listOf(WHITE), Feedback.Outcome.IN_PROGRESS)
            ),
            Arguments.of(
                "it gives a white peg for each code peg on a wrong position",
                Code("Red", "Green", "Blue", "Red"),
                Code("Purple", "Red", "Red", "Purple"),
                Feedback(listOf(WHITE, WHITE), Feedback.Outcome.IN_PROGRESS)
            )
        )
    }

    @Test
    fun `the game is won if the secret is guessed`() {
        val game = listOf(GameStarted(gameId, secret, totalAttempts, availablePegs))

        execute(MakeGuess(gameId, secret), game) shouldSucceedWith listOf(
            GuessMade(
                gameId, Guess(
                    secret, Feedback(
                        listOf(BLACK, BLACK, BLACK, BLACK), Feedback.Outcome.WON
                    )
                )
            ),
            GameWon(gameId)
        )
    }

    @Test
    fun `the game can no longer be played once it's won`() {
        val game = listOf<GameEvent>(GameStarted(gameId, secret, totalAttempts, availablePegs))

        val update = execute(MakeGuess(gameId, secret), game)
        val updatedGame = game + update.getOrElse { emptyList() }

        execute(MakeGuess(gameId, secret), updatedGame) shouldFailWith
                GameAlreadyWon(gameId)
    }

    @Test
    fun `the game is lost if the secret is not guessed within the number of attempts`() {
        val wrongCode = Code("Purple", "Purple", "Purple", "Purple")
        val game = listOf(
            GameStarted(gameId, secret, 3, availablePegs),
            GuessMade(gameId, Guess(wrongCode, Feedback(listOf(), Feedback.Outcome.IN_PROGRESS))),
            GuessMade(gameId, Guess(wrongCode, Feedback(listOf(), Feedback.Outcome.IN_PROGRESS))),
        )
        execute(MakeGuess(gameId, wrongCode), game) shouldSucceedWith listOf(
            GuessMade(gameId, Guess(wrongCode, Feedback(listOf(), Feedback.Outcome.LOST))),
            GameLost(gameId)
        )
    }

    @Test
    fun `the game can no longer be played once it's lost`() {
        val wrongCode = Code("Purple", "Purple", "Purple", "Purple")
        val game = listOf<GameEvent>(GameStarted(gameId, secret, 1, availablePegs))

        val update = execute(MakeGuess(gameId, wrongCode), game)
        val updatedGame = game + update.getOrElse { emptyList() }

        execute(MakeGuess(gameId, secret), updatedGame) shouldFailWith
                GameAlreadyLost(gameId)
    }

    @Test
    fun `the game cannot be played if it was not started`() {
        val code = Code("Red", "Purple", "Red", "Purple")
        val game = emptyList<GameEvent>()

        execute(MakeGuess(gameId, code), game) shouldFailWith GameNotStarted(gameId)
    }

    @Test
    fun `the guess size cannot be shorter than the secret`() {
        val code = Code("Purple", "Purple", "Purple")
        val game = listOf<GameEvent>(GameStarted(gameId, secret, 12, availablePegs))

        execute(MakeGuess(gameId, code), game) shouldFailWith GuessTooShort(gameId, code, secret.length)
    }

    @Test
    fun `the guess size cannot be longer than the secret`() {
        val code = Code("Purple", "Purple", "Purple", "Purple", "Purple")
        val game = listOf<GameEvent>(GameStarted(gameId, secret, 12, availablePegs))

        execute(MakeGuess(gameId, code), game) shouldFailWith GuessTooLong(gameId, code, secret.length)
    }

    @Test
    fun `it rejects pegs that the game was not started with`() {
        val secret = Code("Red", "Green", "Blue", "Yellow")
        val availablePegs = setOfPegs("Red", "Green", "Blue")
        val game = listOf<GameEvent>(GameStarted(gameId, secret, 12, availablePegs))

        execute(MakeGuess(gameId, secret), game) shouldFailWith InvalidPegInGuess(gameId, secret, availablePegs)
    }
}
