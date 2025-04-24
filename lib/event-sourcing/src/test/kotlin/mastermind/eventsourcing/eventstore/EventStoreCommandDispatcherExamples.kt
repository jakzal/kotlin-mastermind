package mastermind.eventsourcing.eventstore

import arrow.core.*
import kotlinx.coroutines.test.runTest
import mastermind.eventsourcing.eventstore.EventSourcingError.ExecutionError
import mastermind.eventsourcing.fixtures.TestCommand
import mastermind.eventsourcing.fixtures.TestError
import mastermind.eventsourcing.fixtures.TestEvent
import mastermind.eventstore.EventStore
import mastermind.eventstore.InMemoryEventStore
import mastermind.eventstore.Stream.LoadedStream
import mastermind.testkit.assertions.shouldFailWith
import mastermind.testkit.assertions.shouldSucceedWith
import org.junit.jupiter.api.Test

class EventStoreCommandDispatcherExamples {
    private val eventStore: EventStore<TestEvent> = InMemoryEventStore()
    private val expectedEvent = TestEvent("ABC")
    private val streamNameResolver = { _: TestCommand -> "Stream:ABC" }
    private val outcomeProducer: (NonEmptyList<TestEvent>) -> String = { events -> events.head.id }

    @Test
    fun `it appends to the event store all the events created in reaction to the command`() = runTest {
        val dispatcher = EventStoreCommandDispatcher(
            eventStore,
            { _, _ -> nonEmptyListOf(expectedEvent).right() },
            streamNameResolver,
            outcomeProducer
        )

        dispatcher(TestCommand("ABC")) shouldSucceedWith "ABC"
        eventStore.load("Stream:ABC") shouldSucceedWith LoadedStream("Stream:ABC", 1L, nonEmptyListOf(expectedEvent))
    }

    @Test
    fun `it produces an outcome`() = runTest {
        givenEventsExist(nonEmptyListOf(TestEvent("123"), TestEvent("456")))

        val outcomeProducer: (NonEmptyList<TestEvent>) -> String = { events ->
            events.map { it.id }.joinToString(",")
        }
        val dispatcher = EventStoreCommandDispatcher(
            eventStore,
            { _, _ -> nonEmptyListOf(expectedEvent).right() },
            streamNameResolver,
            outcomeProducer
        )

        dispatcher(TestCommand("ABC")) shouldSucceedWith "123,456,ABC"
    }

    @Test
    fun `it returns the event store error in case of execution failure`() = runTest {
        val dispatcher = EventStoreCommandDispatcher(
            eventStore,
            { _, _ -> TestError("Execution failed.").left() },
            streamNameResolver,
            outcomeProducer
        )

        dispatcher(TestCommand("ABC")) shouldFailWith ExecutionError(TestError("Execution failed."))
    }

    private suspend fun givenEventsExist(existingEvents: NonEmptyList<TestEvent>) =
        eventStore.loadToAppend("Stream:ABC") {
            existingEvents.right()
        }.getOrElse { e -> throw RuntimeException("Failed to persist events $e.") }
}

