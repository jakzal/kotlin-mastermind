package mastermind.command.journal

import arrow.core.NonEmptyList
import arrow.core.left
import arrow.core.nonEmptyListOf
import arrow.core.raise.either
import arrow.core.right
import kotlinx.coroutines.test.runTest
import mastermind.eventsourcing.Apply
import mastermind.eventsourcing.Execute
import mastermind.eventsourcing.journal.Invoker
import mastermind.eventsourcing.journal.JournalCommandHandler
import mastermind.eventsourcing.journal.NoStateInvoker
import mastermind.journal.*
import mastermind.journal.JournalError.ExecutionError
import mastermind.journal.Stream.LoadedStream
import mastermind.testkit.assertions.shouldBe
import mastermind.testkit.assertions.shouldFailWith
import mastermind.testkit.assertions.shouldReturn
import mastermind.testkit.assertions.shouldSucceedWith
import org.junit.jupiter.api.Test


class JournalCommandHandlerExamples {
    private val journal: Journal<TestEvent, TestError> = InMemoryJournal()
    private val updateStream = with(journal) { createUpdateStream() }
    private val loadStream = with(journal) { createLoadStream() }
    private val expectedEvent = TestEvent("ABC")
    private val streamNameResolver = { _: TestCommand -> "Stream:ABC" }

    @Test
    fun `it appends events created in reaction to the command to the journal`() = runTest {
        val execute: Execute<TestCommand, NonEmptyList<TestEvent>?, TestError, TestEvent> =
            { _: TestCommand, _: NonEmptyList<TestEvent>? ->
                either {
                    nonEmptyListOf(expectedEvent)
                }
            }
        val handler = with(updateStream) {
            JournalCommandHandler(
                NoStateInvoker(execute),
                streamNameResolver
            ) { events -> events.head.id }
        }

        handler(TestCommand("ABC")) shouldReturn "ABC".right()
        loadStream("Stream:ABC") shouldSucceedWith LoadedStream("Stream:ABC", 1L, nonEmptyListOf(expectedEvent))
    }

    @Test
    fun `it makes outcome available to the command executor`() = runTest {
        val execute: Execute<TestCommand, NonEmptyList<TestEvent>?, TestError, TestEvent> =
            { _: TestCommand, state: NonEmptyList<TestEvent>? ->
                either {
                    nonEmptyListOf(expectedEvent).also {
                        state shouldBe nonEmptyListOf(TestEvent("123"), TestEvent("456"))
                    }
                }
            }

        updateStream("Stream:ABC") {
            append(TestEvent("123"), TestEvent("456"))
        }

        val handler = with(updateStream) {
            JournalCommandHandler(
                NoStateInvoker(execute),
                streamNameResolver
            ) { events -> events.map { it.id }.joinToString(",") }
        }

        handler(TestCommand("ABC")) shouldSucceedWith "123,456,ABC"
    }

    @Test
    fun `it reconstitutes any state from the loaded list of events`() = runTest {
        val applyEvent: Apply<TestState?, TestEvent> = { state, event ->
            state?.let { TestState(it.history + event.id) } ?: TestState(listOf(event.id))
        }
        val execute: Execute<TestCommand, TestState?, TestError, TestEvent> =
            { _: TestCommand, state: TestState? ->
                either {
                    nonEmptyListOf(expectedEvent).also {
                        state shouldBe TestState(listOf("987", "654"))
                    }
                }
            }

        updateStream("Stream:ABC") {
            append(TestEvent("987"), TestEvent("654"))
        }

        val handler = with(updateStream) {
            JournalCommandHandler(
                Invoker(applyEvent, execute, { null }),
                streamNameResolver
            ) { events -> events }
        }

        handler(TestCommand("ABC")) shouldSucceedWith listOf(TestEvent("987"), TestEvent("654"), expectedEvent)
    }

    @Test
    fun `it returns journal error in case execute fails`() = runTest {
        val execute: Execute<TestCommand, NonEmptyList<TestEvent>?, TestError, TestEvent> =
            { _: TestCommand, _: NonEmptyList<TestEvent>? -> TestError("Execution failed.").left() }
        val handler = with(updateStream) {
            JournalCommandHandler(
                NoStateInvoker(execute),
                streamNameResolver
            ) { events -> events.head.id }
        }

        handler(TestCommand("ABC")) shouldFailWith ExecutionError(TestError("Execution failed."))
    }

    private data class TestCommand(val id: String)
    private data class TestEvent(val id: String)
    private data class TestError(val cause: String)
    private data class TestState(val history: List<String>)
}
