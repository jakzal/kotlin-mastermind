package mastermind.game.journal

import arrow.core.left
import arrow.core.nonEmptyListOf
import arrow.core.right
import kotlinx.coroutines.test.runTest
import mastermind.game.journal.JournalContract.TestEvent.Event1
import mastermind.game.journal.JournalContract.TestEvent.Event2
import mastermind.game.testkit.shouldBe
import org.junit.jupiter.api.Test

abstract class JournalContract {
    private val journal: Journal<TestEvent> get() = createJournal()

    protected abstract fun createJournal(): Journal<TestEvent>

    @Test
    fun `it persists created events to a new stream`() = runTest {
        val result = journal.create("stream:1") {
            nonEmptyListOf(Event1("ABC"), Event2("ABC", "Event 2")).right()
        }

        result shouldBe listOf(Event1("ABC"), Event2("ABC", "Event 2")).right()
    }

    @Test
    fun `it returns the execution error`() = runTest {
        val result = journal.create("stream:2") {
            TestFailure("Command failed.").left()
        }

        result shouldBe ExecutionFailure(TestFailure("Command failed.")).left()
    }

    protected sealed interface TestEvent {
        val id: String

        data class Event1(override val id: String) : TestEvent
        data class Event2(override val id: String, val name: String) : TestEvent
    }

    protected data class TestFailure(val cause: String)
}
