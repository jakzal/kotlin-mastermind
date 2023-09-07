package mastermind.game.journal

import arrow.core.NonEmptyList
import arrow.core.nonEmptyListOf
import arrow.core.raise.either
import arrow.core.right
import kotlinx.coroutines.test.runTest
import mastermind.game.testkit.shouldBe
import mastermind.game.testkit.shouldReturn
import mastermind.game.testkit.shouldSucceedWith
import mastermind.journal.InMemoryJournal
import mastermind.journal.Stream.LoadedStream
import mastermind.journal.append
import org.junit.jupiter.api.Test

class JournalCommandHandlerExamples {
    private val journal = InMemoryJournal<TestEvent, TestFailure>()

    @Test
    fun `it appends events created in reaction to the command to the journal`() = runTest {
        val expectedEvent = TestEvent("ABC")
        val streamNameResolver = { _: TestCommand -> "Stream:ABC" }
        val execute: Execute<TestCommand, TestEvent, NonEmptyList<TestEvent>, TestFailure> =
            { _: TestCommand, _: NonEmptyList<TestEvent>? ->
                either {
                    nonEmptyListOf(expectedEvent)
                }
            }
        val handler = with(journal) {
            JournalCommandHandler(execute, streamNameResolver) { events -> events.head.id }
        }

        handler(TestCommand("ABC")) shouldReturn "ABC".right()
        journal.load("Stream:ABC") shouldSucceedWith LoadedStream("Stream:ABC", 1L, nonEmptyListOf(expectedEvent))
    }

    @Test
    fun `it makes result available to the command executor`() = runTest {
        val expectedEvent = TestEvent("ABC")
        val streamNameResolver = { _: TestCommand -> "Stream:ABC" }
        val execute: Execute<TestCommand, TestEvent, NonEmptyList<TestEvent>, TestFailure> =
            { _: TestCommand, state: NonEmptyList<TestEvent>? ->
                either {
                    nonEmptyListOf(expectedEvent).also {
                        state shouldBe nonEmptyListOf(TestEvent("123"), TestEvent("456"))
                    }
                }
            }

        journal.stream("Stream:ABC") {
            append(TestEvent("123"), TestEvent("456"))
        }

        val handler = with(journal) {
            JournalCommandHandler(execute, streamNameResolver) { events -> events.map { it.id }.joinToString(",") }
        }

        handler(TestCommand("ABC")) shouldReturn "123,456,ABC".right()
    }

    private data class TestCommand(val id: String)
    private data class TestEvent(val id: String)
    private data class TestFailure(val cause: String)
}
