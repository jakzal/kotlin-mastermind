package mastermind.eventstore

import arrow.core.getOrElse
import arrow.core.nonEmptyListOf
import kotlinx.coroutines.test.runTest
import mastermind.eventstore.EventStoreContract.TestEvent.Event1
import mastermind.eventstore.EventStoreContract.TestEvent.Event2
import mastermind.eventstore.EventStoreError.StreamNotFound
import mastermind.eventstore.EventStoreError.VersionConflict
import mastermind.eventstore.Stream.LoadedStream
import mastermind.testkit.assertions.shouldBeFailureOf
import mastermind.testkit.assertions.shouldBeSuccessOf
import mastermind.testkit.assertions.shouldFailWith
import mastermind.testkit.assertions.shouldSucceedWith
import org.junit.jupiter.api.Test

abstract class EventStoreContract(
    private val eventStore: EventStore<TestEvent>
) {
    private val streamName = UniqueSequence { index -> "stream:$index" }()

    @Test
    fun `it returns StreamNotFound error if stream does not exist`() = runTest {
        eventStore.load(streamName) shouldFailWith StreamNotFound(streamName)
    }

    @Test
    fun `it loads events from a stream`() = runTest {
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

    @Test
    fun `it persists events to a new stream`() = runTest {
        val result = eventStore.append(
            emptyStream<TestEvent>(streamName)
                .append(Event1("A1"), Event2("A2", "Second event."))
        )

        val expectedStream = LoadedStream(
            streamName,
            2,
            nonEmptyListOf(Event1("A1"), Event2("A2", "Second event."))
        )

        result shouldBeSuccessOf expectedStream
        loadEvents(streamName) shouldSucceedWith expectedStream
    }

    @Test
    fun `it appends events to an existing stream`() = runTest {
        val existingStream = givenEventsExist(
            streamName,
            Event1("ABC"),
            Event2("ABC", "Event 2")
        )

        val result = eventStore.append(
            updatedStream(existingStream, Event1("DEF"), Event2("DEF", "Event 2 DEF."))
        )

        val expectedStream = LoadedStream(
            streamName,
            4,
            nonEmptyListOf(
                Event1("ABC"),
                Event2("ABC", "Event 2"),
                Event1("DEF"),
                Event2("DEF", "Event 2 DEF.")
            )
        )

        result shouldBeSuccessOf expectedStream
        loadEvents(streamName) shouldSucceedWith expectedStream
    }

    @Test
    fun `it returns an error if version conflict arises during the write`() = runTest {
        val existingStream = givenEventsExist(
            streamName,
            Event1("ABC"),
            Event2("ABC", "Event 2")
        )

        // We're cheating a bit here as instead of using the provided stream
        // we're using the previously loaded one to simulate concurrent reads.
        val result1 = eventStore.append(
            updatedStream(existingStream, Event1("DEF"), Event2("DEF", "Event 2 DEF."))
        )
        val result2 = eventStore.append(
            updatedStream(existingStream, Event1("XYZ"), Event2("XYZ", "Event 2 XYZ."))
        )

        val expectedStream = LoadedStream(
            streamName,
            4,
            nonEmptyListOf(
                Event1("ABC"),
                Event2("ABC", "Event 2"),
                Event1("DEF"),
                Event2("DEF", "Event 2 DEF.")
            )
        )

        result1 shouldBeSuccessOf expectedStream
        result2 shouldBeFailureOf VersionConflict(streamName, 2, 4)
        loadEvents(streamName) shouldSucceedWith expectedStream
    }

    private suspend fun loadEvents(streamName: StreamName) = eventStore.load(streamName)

    private suspend fun givenEventsExist(streamName: StreamName, event: TestEvent, vararg events: TestEvent) =
        eventStore.append(updatedStream(streamName, event, *events))
            .getOrElse { e -> throw RuntimeException("Failed to persist events $e.") }

    sealed interface TestEvent {
        val id: String

        data class Event1(override val id: String) : TestEvent
        data class Event2(override val id: String, val name: String) : TestEvent
    }
}
