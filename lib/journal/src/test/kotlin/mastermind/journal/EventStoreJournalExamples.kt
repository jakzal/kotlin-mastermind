package mastermind.journal

import kotlinx.coroutines.test.runTest
import mastermind.journal.EventStoreJournalExamples.TestEvent.Event1
import mastermind.journal.EventStoreJournalExamples.TestEvent.Event2
import mastermind.journal.JournalFailure.EventStoreFailure.StreamNotFound
import mastermind.testkit.assertions.shouldFailWith
import mastermind.testkit.assertions.shouldSucceedWith
import org.junit.jupiter.api.Test

class EventStoreJournalExamples {
    private val eventStore: EventStore<TestEvent, TestFailure> = InMemoryEventStore()
    private val journal = with(eventStore) { EventStoreJournal() }
    private val streamName = UniqueSequence { index -> "stream:$index" }()


    @Test
    fun `it loads a stream from the event store`() = runTest {
        eventStore.append(updatedStream(streamName, Event1("A1"), Event2("A2", "Event two.")))

        journal.load(streamName) shouldSucceedWith loadedStream(
            streamName,
            Event1("A1"),
            Event2("A2", "Event two.")
        )
    }

    @Test
    fun `it returns an error if the stream is not found in the event store`() = runTest {
        journal.load(streamName) shouldFailWith StreamNotFound(streamName)
    }

    private sealed interface TestEvent {
        val id: String

        data class Event1(override val id: String) : TestEvent
        data class Event2(override val id: String, val name: String) : TestEvent
    }

    private data class TestFailure(val cause: String)
}