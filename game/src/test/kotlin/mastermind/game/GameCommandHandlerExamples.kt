package mastermind.game

import arrow.core.Either
import arrow.core.NonEmptyList
import arrow.core.nonEmptyListOf
import arrow.core.raise.either
import arrow.core.right
import kotlinx.coroutines.test.runTest
import mastermind.game.Journal.JournalFailure
import mastermind.game.testkit.anyGameId
import mastermind.game.testkit.anySecret
import mastermind.game.testkit.shouldBe
import mastermind.game.testkit.shouldReturn
import org.junit.jupiter.api.Test

class GameCommandHandlerExamples {
    @Test
    fun `it appends events created in reaction to the JoinGame command to the journal`() = runTest {
        val gameId = anyGameId()
        val secret = anySecret()
        val totalAttempts = 8
        with(object : Journal<GameEvent> {
            override suspend fun create(
                streamName: StreamName,
                action: () -> NonEmptyList<GameEvent>
            ): Either<JournalFailure, NonEmptyList<GameEvent>> = either {
                action().also {
                    streamName shouldBe "Mastermind:${gameId.value}"
                }
            }
        }) {
            execute(JoinGame(gameId, secret, totalAttempts)) shouldReturn listOf(
                GameStarted(gameId, secret, totalAttempts)
            ).right()
        }
    }
}

context(Journal<GameEvent>)
private suspend fun execute(command: JoinGame): Either<JournalFailure, NonEmptyList<GameEvent>> {
    return create("Mastermind:${command.gameId.value}") {
        nonEmptyListOf(GameStarted(command.gameId, command.secret, command.totalAttempts))
    }
}

typealias StreamName = String

interface Journal<EVENT : Any> {
    suspend fun create(
        streamName: StreamName,
        action: () -> NonEmptyList<EVENT>
    ): Either<JournalFailure, NonEmptyList<EVENT>>

    sealed interface JournalFailure {
        data class EventStoreFailure(val cause: Throwable) : JournalFailure
    }
}
