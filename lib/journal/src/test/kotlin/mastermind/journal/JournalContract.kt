package mastermind.journal

import arrow.atomic.AtomicInt
import arrow.core.left
import arrow.core.nonEmptyListOf
import arrow.core.right
import kotlinx.coroutines.test.runTest
import mastermind.journal.JournalContract.TestEvent.Event1
import mastermind.journal.JournalContract.TestEvent.Event2
import mastermind.journal.JournalFailure.EventStoreFailure.StreamNotFound
import mastermind.journal.JournalFailure.ExecutionFailure
import mastermind.journal.Stream.LoadedStream
import mastermind.journal.Stream.UpdatedStream
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

abstract class JournalContract {
    protected abstract fun journal(): Journal<TestEvent>

    protected abstract suspend fun loadEvents(streamName: StreamName): List<TestEvent>

    private val streamName = UniqueSequence { index -> "stream:$index" }()

    @Test
    fun `it persists events to a new stream`() = runTest {
        val result = journal().stream(streamName) {
            append(
                Event1("ABC"),
                Event2("ABC", "Event 2")
            )
        }

        result shouldBeSuccessOf LoadedStream(streamName, 2, nonEmptyListOf(Event1("ABC"), Event2("ABC", "Event 2")))
        loadEvents(streamName) shouldReturn listOf(Event1("ABC"), Event2("ABC", "Event 2"))
    }

    @Test
    fun `it persists generated events to a new stream`() = runTest {
        val result = journal().stream(streamName) {
            append {
                nonEmptyListOf(
                    Event1("ABC"),
                    Event2("ABC", "Event 2")
                ).right()
            }
        }

        result shouldBeSuccessOf LoadedStream(streamName, 2, nonEmptyListOf(Event1("ABC"), Event2("ABC", "Event 2")))
        loadEvents(streamName) shouldReturn listOf(Event1("ABC"), Event2("ABC", "Event 2"))
    }

    @Test
    fun `it returns the execution error`() = runTest {
        val result = journal().stream(streamName) {
            TestFailure("Command failed.").left()
        }

        result shouldBeFailureOf ExecutionFailure(TestFailure("Command failed."))
        loadEvents(streamName) shouldReturn emptyList()
    }

    @Test
    fun `it loads events from a stream`() = runTest {
        journal().stream(streamName) {
            append(
                Event1("ABC"),
                Event2("ABC", "Event 2")
            )
        }

        journal().load(streamName) shouldSucceedWith LoadedStream(
            streamName,
            2L,
            nonEmptyListOf(Event1("ABC"), Event2("ABC", "Event 2"))
        )
    }

    @Test
    fun `it returns an error if the stream to load is not found`() = runTest {
        journal().load(streamName) shouldFailWith StreamNotFound(streamName)
    }

    @Test
    fun `it appends events to an existing stream`() = runTest {
        journal().stream(streamName) {
            append(
                Event1("ABC"), Event2("ABC", "Event 2")
            )
        }
        val result = journal().stream(streamName) {
            UpdatedStream(
                this.streamName,
                this.streamVersion,
                this.events,
                nonEmptyListOf(Event1("XYZ"), Event2("XYZ", "Event XYZ"))
            ).right()
        }

        result shouldBeSuccessOf LoadedStream(
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

    protected sealed interface TestEvent {
        val id: String

        data class Event1(override val id: String) : TestEvent
        data class Event2(override val id: String, val name: String) : TestEvent
    }

    protected data class TestFailure(val cause: String)
}

private class UniqueSequence<T>(
    private val nextItem: (Int) -> T
) {
    companion object {
        private val streamCount = AtomicInt(0)
    }

    operator fun invoke(): T = nextItem(streamCount.incrementAndGet())
}

infix fun <T> T?.shouldBe(expected: T?) {
    Assertions.assertEquals(expected, this)
}

infix fun <T> T?.shouldReturn(expected: T?) = shouldBe(expected)

infix fun <T> T?.shouldSucceedWith(expected: T?) = shouldBe(expected.right())
infix fun <T> T?.shouldFailWith(expected: T?) = shouldBe(expected.left())

infix fun <T> T?.shouldBeSuccessOf(expected: T?) = shouldBe(expected.right())
infix fun <T> T?.shouldBeFailureOf(expected: T?) = shouldBe(expected.left())
