package mastermind.game

import arrow.core.Either
import arrow.core.NonEmptyList
import arrow.core.raise.either
import arrow.core.right
import kotlinx.coroutines.test.runTest
import mastermind.game.testkit.anyGameId
import mastermind.game.testkit.anySecret
import mastermind.game.testkit.shouldBe
import mastermind.game.testkit.shouldReturn
import org.junit.jupiter.api.Test

class JournalCommandHandlerExamples {
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
            val command = JoinGame(gameId, secret, totalAttempts)
            val expectedEvent = GameStarted(gameId, secret, totalAttempts)
            val streamNameResolver = { c: GameCommand -> "Mastermind:${c.gameId.value}" }
            JournalCommandHandler(::execute, streamNameResolver)(command) shouldReturn listOf(expectedEvent).right()
        }
    }
}

class JournalCommandHandler<COMMAND : Any, EVENT : Any, FAILURE : Any>(
    private val execute: Execute<COMMAND, EVENT, FAILURE>,
    private val streamNameResolver: (COMMAND) -> String
) {
    context(Journal<EVENT>)
    suspend operator fun invoke(command: COMMAND): Either<JournalFailure, NonEmptyList<EVENT>> {
        return create(streamNameResolver(command)) {
            execute(command).getOrNull()!!
        }
    }
}

typealias StreamName = String

typealias Execute<COMMAND, EVENT, FAILURE> = (COMMAND) -> Either<FAILURE, NonEmptyList<EVENT>>

interface Journal<EVENT : Any> {
    suspend fun create(
        streamName: StreamName,
        action: () -> NonEmptyList<EVENT>
    ): Either<JournalFailure, NonEmptyList<EVENT>>
}

sealed interface JournalFailure
data class EventStoreFailure(val cause: Throwable) : JournalFailure
