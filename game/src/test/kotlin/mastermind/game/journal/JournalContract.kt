package mastermind.game.journal

import arrow.core.left
import arrow.core.nonEmptyListOf
import arrow.core.right
import kotlinx.coroutines.test.runTest
import mastermind.game.journal.JournalContract.TestEvent.Event1
import mastermind.game.journal.JournalContract.TestEvent.Event2
import mastermind.game.testkit.shouldBe
import mastermind.game.testkit.shouldReturn
import org.junit.jupiter.api.Test

abstract class JournalContract {
    protected abstract fun journal(): Journal<TestEvent>

    protected abstract suspend fun loadEvents(streamName: StreamName): List<TestEvent>

    @Test
    fun `it persists created events to a new stream`() = runTest {
        val result = journal().create("stream:1") {
            nonEmptyListOf(Event1("ABC"), Event2("ABC", "Event 2")).right()
        }

        result shouldBe listOf(Event1("ABC"), Event2("ABC", "Event 2")).right()
        loadEvents("stream:1") shouldReturn listOf(Event1("ABC"), Event2("ABC", "Event 2"))
    }

    @Test
    fun `it returns the execution error`() = runTest {
        val result = journal().create("stream:2") {
            TestFailure("Command failed.").left()
        }

        result shouldBe ExecutionFailure(TestFailure("Command failed.")).left()
        loadEvents("stream:2") shouldReturn emptyList()
    }

    @Test
    fun `it loads events from a stream`() = runTest {
        journal().create("stream:3") {
            nonEmptyListOf(Event1("ABC"), Event2("ABC", "Event 2")).right()
        }

        journal().load("stream:3") shouldReturn listOf(Event1("ABC"), Event2("ABC", "Event 2")).right()
    }

    @Test
    fun `it returns an error if the stream to load is not found`() = runTest {
        journal().load("stream:4") shouldReturn StreamNotFound("stream:4").left()
    }

    protected sealed interface TestEvent {
        val id: String

        data class Event1(override val id: String) : TestEvent
        data class Event2(override val id: String, val name: String) : TestEvent
    }

    protected data class TestFailure(val cause: String)
}
