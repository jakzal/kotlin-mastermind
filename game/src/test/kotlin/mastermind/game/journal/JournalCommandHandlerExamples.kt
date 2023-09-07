package mastermind.game.journal

import arrow.core.NonEmptyList
import arrow.core.left
import arrow.core.nonEmptyListOf
import arrow.core.raise.either
import arrow.core.right
import kotlinx.coroutines.test.runTest
import mastermind.game.testkit.shouldBe
import mastermind.game.testkit.shouldFailWith
import mastermind.game.testkit.shouldReturn
import mastermind.game.testkit.shouldSucceedWith
import mastermind.journal.InMemoryJournal
import mastermind.journal.JournalFailure.ExecutionFailure
import mastermind.journal.Stream.LoadedStream
import mastermind.journal.append
import org.junit.jupiter.api.Test

class JournalCommandHandlerExamples {
    private val journal = InMemoryJournal<TestEvent, TestFailure>()
    private val expectedEvent = TestEvent("ABC")
    private val streamNameResolver = { _: TestCommand -> "Stream:ABC" }
    private val apply = { state: NonEmptyList<TestEvent>?, event: TestEvent ->
        if (state == null) nonEmptyListOf(event)
        else state + event
    }

    @Test
    fun `it appends events created in reaction to the command to the journal`() = runTest {
        val execute: Execute<TestCommand, TestEvent, NonEmptyList<TestEvent>, TestFailure> =
            { _: TestCommand, _: NonEmptyList<TestEvent>? ->
                either {
                    nonEmptyListOf(expectedEvent)
                }
            }
        val handler = with(journal) {
            JournalCommandHandler(apply, execute, streamNameResolver) { events -> events.head.id }
        }

        handler(TestCommand("ABC")) shouldReturn "ABC".right()
        journal.load("Stream:ABC") shouldSucceedWith LoadedStream("Stream:ABC", 1L, nonEmptyListOf(expectedEvent))
    }

    @Test
    fun `it makes result available to the command executor`() = runTest {
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
            JournalCommandHandler(apply, execute, streamNameResolver) { events -> events.map { it.id }.joinToString(",") }
        }

        handler(TestCommand("ABC")) shouldSucceedWith  "123,456,ABC"
    }

    @Test
    fun `it reconstitutes any state from the loaded list of events`() = runTest {
        val applyEvent: Apply<TestState, TestEvent> = { state, event ->
            state?.let { TestState(it.history + event.id) } ?: TestState(listOf(event.id))
        }
        val execute: Execute<TestCommand, TestEvent, TestState, TestFailure> =
            { _: TestCommand, state: TestState? ->
                either {
                    nonEmptyListOf(expectedEvent).also {
                        state shouldBe TestState(listOf("987", "654"))
                    }
                }
            }

        journal.stream("Stream:ABC") {
            append(TestEvent("987"), TestEvent("654"))
        }

        val handler = with(journal) {
            JournalCommandHandler(applyEvent, execute, streamNameResolver) { events -> events }
        }

        handler(TestCommand("ABC")) shouldSucceedWith listOf(TestEvent("987"), TestEvent("654"), expectedEvent)
    }

    @Test
    fun `it returns journal failure in case execute fails`() = runTest {
        val execute: Execute<TestCommand, TestEvent, NonEmptyList<TestEvent>, TestFailure> =
            { _: TestCommand, _: NonEmptyList<TestEvent>? -> TestFailure("Execution failed.").left() }
        val handler = with(journal) {
            JournalCommandHandler(apply, execute, streamNameResolver) { events -> events.head.id }
        }

        handler(TestCommand("ABC")) shouldFailWith ExecutionFailure(TestFailure("Execution failed."))
    }

    private data class TestCommand(val id: String)
    private data class TestEvent(val id: String)
    private data class TestFailure(val cause: String)
    private data class TestState(val history: List<String>)
}
