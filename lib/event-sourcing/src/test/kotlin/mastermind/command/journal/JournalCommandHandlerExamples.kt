package mastermind.command.journal

import arrow.core.left
import arrow.core.nonEmptyListOf
import arrow.core.right
import kotlinx.coroutines.test.runTest
import mastermind.command.fixtures.TestCommand
import mastermind.command.fixtures.TestError
import mastermind.command.fixtures.TestEvent
import mastermind.eventsourcing.journal.JournalCommandHandler
import mastermind.journal.*
import mastermind.journal.JournalError.ExecutionError
import mastermind.journal.Stream.LoadedStream
import mastermind.testkit.assertions.shouldBe
import mastermind.testkit.assertions.shouldFailWith
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
        val handler = with(updateStream) {
            JournalCommandHandler(
                { _, _ -> nonEmptyListOf(expectedEvent).right() },
                streamNameResolver
            ) { events -> events.head.id }
        }

        handler(TestCommand("ABC")) shouldSucceedWith "ABC"
        loadStream("Stream:ABC") shouldSucceedWith LoadedStream("Stream:ABC", 1L, nonEmptyListOf(expectedEvent))
    }

    @Test
    fun `it makes outcome available to the command executor`() = runTest {

        updateStream("Stream:ABC") {
            append(TestEvent("123"), TestEvent("456"))
        }

        val handler = with(updateStream) {
            JournalCommandHandler(
                { _, state ->
                    nonEmptyListOf(expectedEvent).right().also {
                        state shouldBe nonEmptyListOf(TestEvent("123"), TestEvent("456"))
                    }
                },
                streamNameResolver
            ) { events -> events.map { it.id }.joinToString(",") }
        }

        handler(TestCommand("ABC")) shouldSucceedWith "123,456,ABC"
    }

    @Test
    fun `it returns journal error in case execute fails`() = runTest {
        val handler = with(updateStream) {
            JournalCommandHandler(
                { _, _ -> TestError("Execution failed.").left() },
                streamNameResolver
            ) { events -> events.head.id }
        }

        handler(TestCommand("ABC")) shouldFailWith ExecutionError(TestError("Execution failed."))
    }
}
