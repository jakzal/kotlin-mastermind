package mastermind.game.journal

import arrow.core.Either
import arrow.core.nonEmptyListOf
import arrow.core.raise.either
import arrow.core.right
import kotlinx.coroutines.test.runTest
import mastermind.game.journal.Stream.LoadedStream
import mastermind.game.journal.Stream.UpdatedStream
import mastermind.game.testkit.fake
import mastermind.game.testkit.shouldBe
import mastermind.game.testkit.shouldReturn
import org.junit.jupiter.api.Test

class JournalCommandHandlerExamples {
    @Test
    fun `it appends events created in reaction to the command to the journal`() = runTest {
        val expectedEvent = TestEvent("ABC")
        val streamNameResolver = { _: TestCommand -> "Stream:ABC" }
        val execute: Execute<TestCommand, TestEvent, TestFailure> = { _: TestCommand ->
            either {
                nonEmptyListOf(expectedEvent)
            }
        }
        val handler = with(journalThatOnlyExpectsToCreateStream("Stream:ABC")) {
            JournalCommandHandler(execute, streamNameResolver) { events -> events.head.id }
        }


        handler(TestCommand("ABC")) shouldReturn "ABC".right()
    }

    @Suppress("SameParameterValue")
    private fun journalThatOnlyExpectsToCreateStream(expectedStream: String) = object : Journal<TestEvent> by fake() {
        override suspend fun <FAILURE : Any> stream(
            streamName: StreamName,
            execute: () -> Either<FAILURE, UpdatedStream<TestEvent>>
        ): Either<JournalFailure<FAILURE>, LoadedStream<TestEvent>> =
            either {
                execute()
                    .getOrNull()!!
                    .let { LoadedStream(streamName, it.streamVersion, it.eventsToAppend) }
                    .also { streamName shouldBe expectedStream }
            }
    }

    private data class TestCommand(val id: String)
    private data class TestEvent(val id: String)
    private data class TestFailure(val cause: String)
}
