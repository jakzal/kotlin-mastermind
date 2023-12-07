package mastermind.command.eventstore

import arrow.core.*
import kotlinx.coroutines.test.runTest
import mastermind.command.fixtures.TestCommand
import mastermind.command.fixtures.TestError
import mastermind.command.fixtures.TestEvent
import mastermind.eventsourcing.eventstore.EventStoreCommandDispatcher
import mastermind.eventstore.EventStore
import mastermind.eventstore.EventStoreError.ExecutionError
import mastermind.eventstore.InMemoryEventStore
import mastermind.eventstore.Stream.LoadedStream
import mastermind.eventstore.loadToAppend
import mastermind.testkit.assertions.shouldFailWith
import mastermind.testkit.assertions.shouldSucceedWith
import org.junit.jupiter.api.Test


class EventStoreCommandDispatcherExamples {
    private val eventStore: EventStore<TestEvent, TestError> = InMemoryEventStore()
    private val expectedEvent = TestEvent("ABC")
    private val streamNameResolver = { _: TestCommand -> "Stream:ABC" }
    private val outcomeProducer: (NonEmptyList<TestEvent>) -> String = { events -> events.head.id }

    @Test
    fun `it appends to the event store all the events created in reaction to the command`() = runTest {
        with(eventStore) {
            val dispatcher = EventStoreCommandDispatcher(
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
        with(eventStore) {
            givenEventsExist(nonEmptyListOf(TestEvent("123"), TestEvent("456")))

            val outcomeProducer: (NonEmptyList<TestEvent>) -> String = { events ->
                events.map { it.id }.joinToString(",")
            }
            val dispatcher = EventStoreCommandDispatcher(
                { _, _ -> nonEmptyListOf(expectedEvent).right() },
                streamNameResolver,
                outcomeProducer
            )

            dispatcher(TestCommand("ABC")) shouldSucceedWith "123,456,ABC"
        }
    }

    @Test
    fun `it returns the event store error in case of execution failure`() = runTest {
        val dispatcher = with(eventStore) {
            EventStoreCommandDispatcher(
                { _, _ -> TestError("Execution failed.").left() },
                streamNameResolver,
                outcomeProducer
            )
        }

        dispatcher(TestCommand("ABC")) shouldFailWith ExecutionError(TestError("Execution failed."))
    }

    context(EventStore<TestEvent, TestError>)
    private suspend fun givenEventsExist(existingEvents: NonEmptyList<TestEvent>) =
        loadToAppend("Stream:ABC") {
            existingEvents.right()
        }.getOrElse { e -> throw RuntimeException("Failed to persist events $e.") }
}
