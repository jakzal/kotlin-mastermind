package mastermind.journal.eventstoredb

import com.eventstore.dbclient.*
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlinx.coroutines.future.await
import kotlinx.coroutines.test.runTest
import mastermind.journal.eventstoredb.Event.TestEvent1
import mastermind.journal.eventstoredb.Event.TestEvent2
import mastermind.testkit.testcontainers.eventstoredb.EventStoreDbContainer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@Testcontainers(disabledWithoutDocker = true)
@Tag("learning")
@Tag("io")
class LearnEventStoreDbExamples {
    companion object {
        @Container
        private val eventStoreDb: EventStoreDbContainer<*> = EventStoreDbContainer()
    }

    @Test
    fun `it reads appended events`() = runTest {
        val event1 = TestEvent1("Event 1")
        val event2 = TestEvent2("Event 2", 1)
        val event3 = TestEvent2("Event 2", 24)
        appendToStream("test-stream-1", event1, event2, event3)

        val result: ReadResult = eventStoreClient()
            .readStream("test-stream-1", ReadStreamOptions.get())
            .await()

        assertEquals(3, result.events.size)
        assertEquals(listOf(event1, event2, event3), result.events.map(::mapToTestEvent).toList())
    }

    private fun eventStoreClient(): EventStoreDBClient = EventStoreDBClient.create(
        EventStoreDBConnectionString.parseOrThrow(eventStoreDb.connectionString)
    )

    private suspend fun appendToStream(streamName: String, vararg events: Event): WriteResult =
        eventStoreClient()
            .appendToStream(streamName, events.map(Event::asEventData).listIterator())
            .await()
}

private sealed interface Event {
    data class TestEvent1(val name: String) : Event

    data class TestEvent2(val name: String, val counter: Int) : Event
}

private fun Event.asEventData() =
    EventData.builderAsBinary(this::class.java.typeName, jacksonObjectMapper().writeValueAsBytes(this)).build()

private fun mapToTestEvent(event: ResolvedEvent): Event =
    jacksonObjectMapper().readValue(
        event.event.eventData.decodeToString(),
        Class.forName(event.event.eventType)
    ) as Event
