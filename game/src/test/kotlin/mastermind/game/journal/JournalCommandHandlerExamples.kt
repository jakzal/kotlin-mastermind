package mastermind.game.journal

import arrow.core.*
import arrow.core.raise.either
import kotlinx.coroutines.test.runTest
import mastermind.game.journal.Stream.LoadedStream
import mastermind.game.journal.Stream.UpdatedStream
import mastermind.game.testkit.fake
import mastermind.game.testkit.shouldBe
import mastermind.game.testkit.shouldReturn
import org.junit.jupiter.api.Test

class JournalCommandHandlerExamples {
    @Test
    fun `it appends events created in reaction to the command to the journal`() = runTest {
        val expectedEvent = TestEvent("ABC")
        val streamNameResolver = { _: TestCommand -> "Stream:ABC" }
        val execute: Execute<TestCommand, TestEvent, NonEmptyList<TestEvent>, TestFailure> =
            { _: TestCommand, _: NonEmptyList<TestEvent>? ->
                either {
                    nonEmptyListOf(expectedEvent)
                }
            }
        val handler = with(journalThatOnlyExpectsToAppendToStream("Stream:ABC")) {
            JournalCommandHandler(execute, streamNameResolver) { events -> events.head.id }
        }

        handler(TestCommand("ABC")) shouldReturn "ABC".right()
    }

    @Test
    fun `it makes state available to the command executor`() = runTest {
        val expectedEvent = TestEvent("ABC")
        val streamNameResolver = { _: TestCommand -> "Stream:ABC" }
        val execute: Execute<TestCommand, TestEvent, NonEmptyList<TestEvent>, TestFailure> =
            { _: TestCommand, state: NonEmptyList<TestEvent>? ->
                either {
                    nonEmptyListOf(expectedEvent).also {
                        state shouldBe nonEmptyListOf(TestEvent("123"), TestEvent("456"))
                    }
                }
            }
        val handler = with(
            journalThatOnlyExpectsToAppendToStream("Stream:ABC", listOf(TestEvent("123"), TestEvent("456")))
        ) {
            JournalCommandHandler(execute, streamNameResolver) { events -> events.map { it.id }.joinToString(",") }
        }

        handler(TestCommand("ABC")) shouldReturn "123,456,ABC".right()
    }

    @Suppress("SameParameterValue")
    private fun journalThatOnlyExpectsToAppendToStream(
        expectedStream: String,
        existingEvents: List<TestEvent> = emptyList()
    ) = object : Journal<TestEvent> by fake() {
        override suspend fun <FAILURE : Any> stream(
            streamName: StreamName,
            execute: Stream<TestEvent>.() -> Either<FAILURE, UpdatedStream<TestEvent>>
        ): Either<JournalFailure<FAILURE>, LoadedStream<TestEvent>> =
            either {
                existingEvents
                    .toNonEmptyListOrNone()
                    .map {
                        Stream.EmptyStream<TestEvent>(streamName)
                            .append<TestEvent, FAILURE>(it)
                            .getOrNull()!!
                            .toLoadedStream()
                    }
                    .getOrElse { Stream.EmptyStream(streamName) }
                    .execute()
                    .getOrNull()!!
                    .toLoadedStream()
                    .also { streamName shouldBe expectedStream }
            }
    }

    private data class TestCommand(val id: String)
    private data class TestEvent(val id: String)
    private data class TestFailure(val cause: String)
}
