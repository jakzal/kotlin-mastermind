package mastermind.game.journal

import arrow.core.Either
import arrow.core.NonEmptyList
import arrow.core.nonEmptyListOf
import arrow.core.raise.either
import arrow.core.right
import kotlinx.coroutines.test.runTest
import mastermind.game.testkit.shouldBe
import mastermind.game.testkit.shouldReturn
import org.junit.jupiter.api.Test

class JournalCommandHandlerExamples {
    @Test
    fun `it appends events created in reaction to the JoinGame command to the journal`() = runTest {
        val expectedEvent = TestEvent("ABC")
        val streamNameResolver = { _: TestCommand -> "Stream:ABC" }
        val execute: Execute<TestCommand, TestEvent, TestFailure> = { _: TestCommand ->
            either {
                nonEmptyListOf(expectedEvent)
            }
        }
        val handler = JournalCommandHandler(execute, streamNameResolver)

        with(journalThatOnlyExpectsToCreateStream("Stream:ABC")) {
            handler(TestCommand("ABC"))
        } shouldReturn listOf(expectedEvent).right()
    }

    @Suppress("SameParameterValue")
    private fun journalThatOnlyExpectsToCreateStream(expectedStream: String) = object : Journal<TestEvent> {
        override suspend fun create(streamName: StreamName, action: () -> NonEmptyList<TestEvent>)
                : Either<JournalFailure, NonEmptyList<TestEvent>> =
            either {
                action().also { streamName shouldBe expectedStream }
            }
    }

    private data class TestCommand(val id: String)
    private data class TestEvent(val id: String)
    private data class TestFailure(val cause: String)
}
