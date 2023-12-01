package mastermind.command.journal

import arrow.core.*
import kotlinx.coroutines.test.runTest
import mastermind.command.fixtures.TestCommand
import mastermind.command.fixtures.TestError
import mastermind.command.fixtures.TestEvent
import mastermind.eventsourcing.journal.JournalCommandDispatcher
import mastermind.journal.InMemoryJournal
import mastermind.journal.Journal
import mastermind.journal.JournalError.ExecutionError
import mastermind.journal.Stream.LoadedStream
import mastermind.journal.loadToAppend
import mastermind.testkit.assertions.shouldFailWith
import mastermind.testkit.assertions.shouldSucceedWith
import org.junit.jupiter.api.Test


class JournalCommandDispatcherExamples {
    private val journal: Journal<TestEvent, TestError> = InMemoryJournal()
    private val expectedEvent = TestEvent("ABC")
    private val streamNameResolver = { _: TestCommand -> "Stream:ABC" }
    private val outcomeProducer: (NonEmptyList<TestEvent>) -> String = { events -> events.head.id }

    @Test
    fun `it appends to the journal events created in reaction to the command`() = runTest {
        with(journal) {
            val dispatcher = JournalCommandDispatcher(
                { _, _ -> nonEmptyListOf(expectedEvent).right() },
                streamNameResolver,
                outcomeProducer
            )

            dispatcher(TestCommand("ABC")) shouldSucceedWith "ABC"
            load("Stream:ABC") shouldSucceedWith LoadedStream("Stream:ABC", 1L, nonEmptyListOf(expectedEvent))
        }
    }

    @Test
    fun `it produces an outcome`() = runTest {
        with(journal) {
            givenEventsExist(nonEmptyListOf(TestEvent("123"), TestEvent("456")))

            val outcomeProducer: (NonEmptyList<TestEvent>) -> String = { events ->
                events.map { it.id }.joinToString(",")
            }
            val dispatcher = JournalCommandDispatcher(
                { _, _ -> nonEmptyListOf(expectedEvent).right() },
                streamNameResolver,
                outcomeProducer
            )

            dispatcher(TestCommand("ABC")) shouldSucceedWith "123,456,ABC"
        }
    }

    @Test
    fun `it returns journal error in case of execution failure`() = runTest {
        val dispatcher = with(journal) {
            JournalCommandDispatcher(
                { _, _ -> TestError("Execution failed.").left() },
                streamNameResolver,
                outcomeProducer
            )
        }

        dispatcher(TestCommand("ABC")) shouldFailWith ExecutionError(TestError("Execution failed."))
    }

    context(Journal<TestEvent, TestError>)
    private suspend fun givenEventsExist(existingEvents: NonEmptyList<TestEvent>) =
        loadToAppend("Stream:ABC") {
            existingEvents.right()
        }.getOrElse { e -> throw RuntimeException("Failed to persist events $e.") }
}
