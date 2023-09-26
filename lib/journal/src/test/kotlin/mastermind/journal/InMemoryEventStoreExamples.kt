package mastermind.journal

import arrow.atomic.Atomic
import arrow.atomic.AtomicInt
import arrow.core.nonEmptyListOf
import mastermind.journal.InMemoryEventStoreExamples.TestEvent.Event1
import mastermind.journal.InMemoryEventStoreExamples.TestEvent.Event2
import mastermind.journal.JournalFailure.EventStoreFailure.StreamNotFound
import mastermind.journal.Stream.*
import mastermind.testkit.assertions.shouldFailWith
import mastermind.testkit.assertions.shouldSucceedWith
import org.junit.jupiter.api.Test

class InMemoryEventStoreExamples {
    private val events = Atomic(mapOf<StreamName, LoadedStream<TestEvent>>())
    private val eventStore = InMemoryEventStore<TestEvent, TestFailure>(events)
    private val streamName = UniqueSequence { index -> "stream:$index" }()

    @Test
    fun `it returns StreamNotFound error if stream does not exist`() {
        eventStore.load(streamName) shouldFailWith StreamNotFound(streamName)
    }

    @Test
    fun `it loads events from a stream`() {
        givenEventsExist(
            streamName,
            Event1("ABC"),
            Event2("ABC", "Event 2")
        )

        eventStore.load(streamName) shouldSucceedWith LoadedStream(
            streamName, 2, nonEmptyListOf(
                Event1("ABC"),
                Event2("ABC", "Event 2")
            )
        )
    }

    private fun givenEventsExist(streamName: StreamName, event: TestEvent, vararg events: TestEvent) =
        eventStore.append(updatedStream(streamName, event, *events))

    private sealed interface TestEvent {
        val id: String

        data class Event1(override val id: String) : TestEvent
        data class Event2(override val id: String, val name: String) : TestEvent
    }

    private data class TestFailure(val cause: String)
}

private fun <EVENT : Any> updatedStream(
    streamName: StreamName,
    event: EVENT,
    vararg events: EVENT
): UpdatedStream<EVENT> =
    EmptyStream<EVENT>(streamName).append<EVENT, Nothing>(event, *events).getOrNull()
        ?: throw RuntimeException("Failed to create an updated stream.")


private class UniqueSequence<T>(
    private val nextItem: (Int) -> T
) {
    companion object {
        private val streamCount = AtomicInt(0)
    }

    operator fun invoke(): T = nextItem(streamCount.incrementAndGet())
}
