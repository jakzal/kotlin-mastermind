package mastermind.game.journal

import arrow.core.Either
import arrow.core.NonEmptyList
import arrow.core.nonEmptyListOf
import arrow.core.right
import kotlinx.coroutines.test.runTest
import mastermind.game.journal.InMemoryJournalExamples.TestEvent.Event1
import mastermind.game.journal.InMemoryJournalExamples.TestEvent.Event2
import mastermind.game.testkit.shouldBe
import org.junit.jupiter.api.Test

class InMemoryJournalExamples {
    private val events = mutableMapOf<String, List<TestEvent>>()
    private val journal = object : Journal<TestEvent> {
        override suspend fun <FAILURE : Any> create(
            streamName: StreamName,
            action: () -> Either<FAILURE, NonEmptyList<TestEvent>>
        ): Either<JournalFailure<FAILURE>, NonEmptyList<TestEvent>> {
            return action().onRight { newEvents -> events[streamName] = newEvents }.getOrNull()!!.right()
        }
    }

    @Test
    fun `it persists created events to a new stream`() = runTest {
        val result = journal.create("stream:1") {
            nonEmptyListOf(Event1("ABC"), Event2("ABC", "Event 2")).right()
        }

        result shouldBe listOf(Event1("ABC"), Event2("ABC", "Event 2")).right()
    }

    private sealed interface TestEvent {
        val id: String

        data class Event1(override val id: String) : TestEvent
        data class Event2(override val id: String, val name: String) : TestEvent
    }
}