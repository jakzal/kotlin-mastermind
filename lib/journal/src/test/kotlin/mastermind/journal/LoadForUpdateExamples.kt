package mastermind.journal

import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import kotlinx.coroutines.test.runTest
import mastermind.journal.JournalError.ExecutionError
import mastermind.journal.JournalError.VersionConflict
import mastermind.journal.LoadForUpdateExamples.TestEvent.Event1
import mastermind.journal.LoadForUpdateExamples.TestEvent.Event2
import mastermind.journal.Stream.UpdatedStream
import mastermind.testkit.assertions.shouldBe
import mastermind.testkit.assertions.shouldBeFailureOf
import mastermind.testkit.assertions.shouldBeSuccessOf
import mastermind.testkit.assertions.shouldSucceedWith
import org.junit.jupiter.api.Test

class LoadForUpdateExamples {
    private val journal: Journal<TestEvent, String> = InMemoryJournal()
    private val streamName = UniqueSequence { index -> "stream:$index" }()

    @Test
    fun `it persists events to a new stream`() = runTest {
        with(journal) {
            val expectedStream = loadedStream(
                streamName,
                Event1("A1"),
                Event2("A2", "Second event.")
            )

            val result = loadForUpdate(streamName) {
                this shouldBe emptyStream(streamName)
                append(
                    Event1("A1"),
                    Event2("A2", "Second event.")
                )
            }

            result shouldBeSuccessOf expectedStream
            load(streamName) shouldSucceedWith expectedStream
        }
    }

    @Test
    fun `it appends events to an existing stream`() = runTest {
        with(journal) {
            val existingStream = givenEventsExist(streamName, Event1("ABC"), Event2("ABC", "Event 2"))
            val expectedStream = loadedStream(
                streamName,
                Event1("ABC"),
                Event2("ABC", "Event 2"),
                Event1("DEF"),
                Event2("DEF", "Event 2 DEF.")
            )

            val result = loadForUpdate(streamName) {
                this shouldBe existingStream
                append(
                    Event1("DEF"),
                    Event2("DEF", "Event 2 DEF.")
                )
            }

            result shouldBeSuccessOf expectedStream
            load(streamName) shouldSucceedWith expectedStream
        }
    }

    @Test
    fun `it returns the journal error on execution failure`() = runTest {
        with(journal) {
            val expectedStream = givenEventsExist(
                streamName,
                Event1("ABC"),
                Event2("ABC", "Event 2")
            )

            val result = loadForUpdate(streamName) {
                "Failed to execute.".left()
            }

            result shouldBeFailureOf ExecutionError("Failed to execute.")
            load(streamName) shouldSucceedWith expectedStream
        }
    }

    @Test
    fun `it returns the journal error on append failure`() = runTest {
        val expectedStream = loadedStream(streamName, Event1("ABC"), Event2("ABC", "Event 2"))

        val failingJournal: Journal<TestEvent, String> = object : Journal<TestEvent, String> {
            override suspend fun load(streamName: StreamName) =
                expectedStream.right()

            override suspend fun append(stream: UpdatedStream<TestEvent>) =
                VersionConflict(streamName, 1, 2).left()

        }

        with(failingJournal) {
            val result = loadForUpdate(streamName) {
                append(Event1("XYZ"), Event2("XYZ", "Event 2 XYZ."))
            }

            result shouldBeFailureOf VersionConflict(streamName, 1, 2)
            load(streamName) shouldSucceedWith expectedStream
        }
    }

    @Test
    fun `it returns the journal error on load failure`() = runTest {
        val failingJournal: Journal<TestEvent, String> = object : Journal<TestEvent, String> {
            override suspend fun load(streamName: StreamName) =
                VersionConflict(streamName, 1, 2).left()

            override suspend fun append(stream: UpdatedStream<TestEvent>) =
                throw RuntimeException("Unexpected call to append.")

        }

        with(failingJournal) {
            val result = loadForUpdate(streamName) {
                append(Event1("XYZ"), Event2("XYZ", "Event 2 XYZ."))
            }

            result shouldBeFailureOf VersionConflict(streamName, 1, 2)
        }
    }

    context(Journal<TestEvent, String>)
    private suspend fun givenEventsExist(streamName: StreamName, event: TestEvent, vararg events: TestEvent) =
        append(updatedStream(streamName, event, *events))
            .getOrElse { e -> throw RuntimeException("Failed to persist events $e.") }


    sealed interface TestEvent {
        val id: String

        data class Event1(override val id: String) : TestEvent
        data class Event2(override val id: String, val name: String) : TestEvent
    }
}