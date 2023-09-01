package mastermind.journal

import arrow.core.left
import arrow.core.nonEmptyListOf
import arrow.core.right
import kotlinx.coroutines.test.runTest
import mastermind.journal.JournalContract.TestEvent.Event1
import mastermind.journal.JournalContract.TestEvent.Event2
import mastermind.journal.JournalFailure.EventStoreFailure.ExecutionFailure
import mastermind.journal.JournalFailure.EventStoreFailure.StreamNotFound
import mastermind.journal.Stream.LoadedStream
import mastermind.journal.Stream.UpdatedStream
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

abstract class JournalContract {
    protected abstract fun journal(): Journal<TestEvent>

    protected abstract suspend fun loadEvents(streamName: StreamName): List<TestEvent>

    @Test
    fun `it persists events to a new stream`() = runTest {
        val result = journal().stream("stream:1a") {
            append(
                Event1("ABC"),
                Event2("ABC", "Event 2")
            )
        }

        result shouldBe LoadedStream("stream:1a", 2, nonEmptyListOf(Event1("ABC"), Event2("ABC", "Event 2"))).right()
        loadEvents("stream:1a") shouldReturn listOf(Event1("ABC"), Event2("ABC", "Event 2"))
    }

    @Test
    fun `it persists generated events to a new stream`() = runTest {
        val result = journal().stream("stream:1b") {
            append {
                nonEmptyListOf(
                    Event1("ABC"),
                    Event2("ABC", "Event 2")
                ).right()
            }
        }

        result shouldBe LoadedStream("stream:1b", 2, nonEmptyListOf(Event1("ABC"), Event2("ABC", "Event 2"))).right()
        loadEvents("stream:1b") shouldReturn listOf(Event1("ABC"), Event2("ABC", "Event 2"))
    }

    @Test
    fun `it returns the execution error`() = runTest {
        val result = journal().stream("stream:2") {
            TestFailure("Command failed.").left()
        }

        result shouldBe ExecutionFailure(TestFailure("Command failed.")).left()
        loadEvents("stream:2") shouldReturn emptyList()
    }

    @Test
    fun `it loads events from a stream`() = runTest {
        journal().stream("stream:3") {
            append(
                Event1("ABC"),
                Event2("ABC", "Event 2")
            )
        }

        journal().load("stream:3") shouldReturn LoadedStream(
            "stream:3",
            2L,
            nonEmptyListOf(Event1("ABC"), Event2("ABC", "Event 2"))
        ).right()
    }

    @Test
    fun `it returns an error if the stream to load is not found`() = runTest {
        journal().load("stream:4") shouldReturn StreamNotFound("stream:4").left()
    }

    @Test
    fun `it appends events to an existing stream`() = runTest {
        journal().stream("stream:5") {
            append(
                Event1("ABC"), Event2("ABC", "Event 2")
            )
        }
        val result = journal().stream("stream:5") {
            UpdatedStream(
                this.streamName,
                this.streamVersion,
                this.events,
                nonEmptyListOf(Event1("XYZ"), Event2("XYZ", "Event XYZ"))
            ).right()
        }

        result shouldBe LoadedStream(
            "stream:5", 4, nonEmptyListOf(
                Event1("ABC"),
                Event2("ABC", "Event 2"),
                Event1("XYZ"),
                Event2("XYZ", "Event XYZ")
            )
        ).right()
        loadEvents("stream:5") shouldReturn listOf(
            Event1("ABC"),
            Event2("ABC", "Event 2"),
            Event1("XYZ"),
            Event2("XYZ", "Event XYZ")
        )
    }

    protected sealed interface TestEvent {
        val id: String

        data class Event1(override val id: String) : TestEvent
        data class Event2(override val id: String, val name: String) : TestEvent
    }

    protected data class TestFailure(val cause: String)
}

infix fun <T> T?.shouldBe(expected: T?) {
    Assertions.assertEquals(expected, this)
}

infix fun <T> T?.shouldReturn(expected: T?) = shouldBe(expected)
