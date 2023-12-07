package mastermind.journal

import arrow.core.getOrElse
import arrow.core.nonEmptyListOf
import kotlinx.coroutines.test.runTest
import mastermind.journal.Entries.LoadedEntries
import mastermind.journal.JournalContract.TestEvent.Event1
import mastermind.journal.JournalContract.TestEvent.Event2
import mastermind.journal.JournalError.StreamNotFound
import mastermind.journal.JournalError.VersionConflict
import mastermind.testkit.assertions.shouldBeFailureOf
import mastermind.testkit.assertions.shouldBeSuccessOf
import mastermind.testkit.assertions.shouldFailWith
import mastermind.testkit.assertions.shouldSucceedWith
import org.junit.jupiter.api.Test

abstract class JournalContract(
    private val journal: Journal<TestEvent, Nothing>
) {
    private val streamName = UniqueSequence { index -> "stream:$index" }()

    @Test
    fun `it returns StreamNotFound error if stream does not exist`() = runTest {
        journal.load(streamName) shouldFailWith StreamNotFound(streamName)
    }

    @Test
    fun `it loads events from a stream`() = runTest {
        givenEventsExist(
            streamName,
            Event1("ABC"),
            Event2("ABC", "Event 2")
        )

        journal.load(streamName) shouldSucceedWith LoadedEntries(
            streamName, 2, nonEmptyListOf(
                Event1("ABC"),
                Event2("ABC", "Event 2")
            )
        )
    }

    @Test
    fun `it persists events to a new stream`() = runTest {
        val result = journal.append(
            updatedEntries(streamName, Event1("A1"), Event2("A2", "Second event."))
        )

        val expectedStream = LoadedEntries(
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

        val result = journal.append(
            updatedEntries(existingStream, Event1("DEF"), Event2("DEF", "Event 2 DEF."))
        )

        val expectedStream = LoadedEntries(
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
        val result1 = journal.append(
            updatedEntries(existingStream, Event1("DEF"), Event2("DEF", "Event 2 DEF."))
        )
        val result2 = journal.append(
            updatedEntries(existingStream, Event1("XYZ"), Event2("XYZ", "Event 2 XYZ."))
        )

        val expectedStream = LoadedEntries(
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

    private suspend fun loadEvents(journalName: JournalName) = journal.load(journalName)

    private suspend fun givenEventsExist(journalName: JournalName, event: TestEvent, vararg events: TestEvent) =
        journal.append(updatedEntries(journalName, event, *events))
            .getOrElse { e -> throw RuntimeException("Failed to persist events $e.") }

    sealed interface TestEvent {
        val id: String

        data class Event1(override val id: String) : TestEvent
        data class Event2(override val id: String, val name: String) : TestEvent
    }
}
