package mastermind.journal

import arrow.core.getOrElse
import arrow.core.left
import arrow.core.nonEmptyListOf
import arrow.core.right
import kotlinx.coroutines.test.runTest
import mastermind.journal.EventStoreJournalExamples.TestEvent.Event1
import mastermind.journal.EventStoreJournalExamples.TestEvent.Event2
import mastermind.testkit.assertions.*
import org.junit.jupiter.api.Test

class EventStoreJournalExamples {
    private val journal = with(InMemoryEventStore<TestEvent, TestFailure>()) {
        EventStoreJournal()
    }

    private val streamName = UniqueSequence { index -> "stream:$index" }()

    @Test
    fun `it persists events to a new stream`() = runTest {
        val result = journal.stream(streamName) {
            append(
                Event1("ABC"),
                Event2("ABC", "Event 2")
            )
        }

        result shouldBeSuccessOf Stream.LoadedStream(
            streamName,
            2,
            nonEmptyListOf(Event1("ABC"), Event2("ABC", "Event 2"))
        )
        loadEvents(streamName) shouldReturn listOf(Event1("ABC"), Event2("ABC", "Event 2"))
    }

    @Test
    fun `it persists generated events to a new stream`() = runTest {
        val result = journal.stream(streamName) {
            append {
                nonEmptyListOf(
                    Event1("ABC"),
                    Event2("ABC", "Event 2")
                ).right()
            }
        }

        result shouldBeSuccessOf Stream.LoadedStream(
            streamName,
            2,
            nonEmptyListOf(Event1("ABC"), Event2("ABC", "Event 2"))
        )
        loadEvents(streamName) shouldReturn listOf(Event1("ABC"), Event2("ABC", "Event 2"))
    }

    @Test
    fun `it returns the execution failure if stream update returns an error`() = runTest {
        val result = journal.stream(streamName) {
            TestFailure("Command failed.").left()
        }

        result shouldBeFailureOf JournalFailure.ExecutionFailure(TestFailure("Command failed."))
        loadEvents(streamName) shouldReturn emptyList()
    }

    @Test
    fun `it loads events from a stream`() = runTest {
        givenEventsExist(streamName, Event1("ABC"), Event2("ABC", "Event 2"))

        journal.load(streamName) shouldSucceedWith Stream.LoadedStream(
            streamName,
            2L,
            nonEmptyListOf(Event1("ABC"), Event2("ABC", "Event 2"))
        )
    }

    @Test
    fun `it returns an error if the stream to load is not found`() = runTest {
        journal.load(streamName) shouldFailWith JournalFailure.EventStoreFailure.StreamNotFound<TestFailure>(streamName)
    }

    @Test
    fun `it appends events to an existing stream`() = runTest {
        givenEventsExist(streamName, Event1("ABC"), Event2("ABC", "Event 2"))

        val result = journal.stream(streamName) {
            Stream.UpdatedStream(
                this.streamName,
                this.streamVersion,
                this.events,
                nonEmptyListOf(Event1("XYZ"), Event2("XYZ", "Event XYZ"))
            ).right()
        }

        result shouldBeSuccessOf Stream.LoadedStream(
            streamName, 4, nonEmptyListOf(
                Event1("ABC"),
                Event2("ABC", "Event 2"),
                Event1("XYZ"),
                Event2("XYZ", "Event XYZ")
            )
        )
        loadEvents(streamName) shouldReturn listOf(
            Event1("ABC"),
            Event2("ABC", "Event 2"),
            Event1("XYZ"),
            Event2("XYZ", "Event XYZ")
        )
    }

    @Test
    fun `it returns a failure if version conflict arises during the write`() = runTest {
        val stream = givenEventsExist(streamName, Event1("ABC"), Event2("ABC", "Event 2"))

        val result1 = journal.stream(streamName) {
            // We're cheating a bit here as instead of using the provided stream
            // we're using the previously loaded one to simulate concurrent reads.
            stream.append(Event1("DEF"), Event1("GHI"))
        }
        val result2 = journal.stream(streamName) {
            stream.append(Event2("XYZ", "Event XYZ"))
        }

        result1 shouldBeSuccessOf Stream.LoadedStream(
            streamName,
            4L,
            nonEmptyListOf(Event1("ABC"), Event2("ABC", "Event 2"), Event1("DEF"), Event1("GHI"))
        )
        result2 shouldBeFailureOf JournalFailure.EventStoreFailure.VersionConflict(streamName, 2, 4)
        loadEvents(streamName) shouldReturn listOf(
            Event1("ABC"),
            Event2("ABC", "Event 2"),
            Event1("DEF"),
            Event1("GHI")
        )
    }

    private suspend fun givenEventsExist(streamName: String, event: TestEvent, vararg events: TestEvent) =
        journal.stream(streamName) {
            append(event, *events)
        }.getOrElse {
            throw RuntimeException("Failed to persist events.")
        }


    private suspend fun loadEvents(streamName: StreamName): List<TestEvent> =
        journal.load(streamName).fold(
            { emptyList() },
            { it.events }
        )

    private sealed interface TestEvent {
        val id: String

        data class Event1(override val id: String) : TestEvent
        data class Event2(override val id: String, val name: String) : TestEvent
    }

    private data class TestFailure(val cause: String)
}