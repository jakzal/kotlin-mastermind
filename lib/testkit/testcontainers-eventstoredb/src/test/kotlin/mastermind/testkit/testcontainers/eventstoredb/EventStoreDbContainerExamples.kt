package mastermind.testkit.testcontainers.eventstoredb

import com.eventstore.dbclient.*
import kotlinx.coroutines.future.await
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.util.*


@Testcontainers(disabledWithoutDocker = true)
@Tag("io")
class EventStoreDbContainerExamples {
    @Container
    private val eventStoreDb: EventStoreDbContainer<*> = EventStoreDbContainer()

    @Test
    fun `it starts a working eventstoredb container`() = runTest {
        val settings = EventStoreDBConnectionString.parseOrThrow(eventStoreDb.connectionString)
        val client = EventStoreDBClient.create(settings)
        val streamName = "test-stream:${UUID.randomUUID()}"
        client.appendToStream(streamName, "TestEvent".asEventData()).await()
        val events = client.readStreamFromStart(streamName).await().events
        assertEquals(listOf("\"TestEvent\""), events.toStringList())
    }
}

private fun String.asEventData(): EventData = EventDataBuilder.json("String", this).build()

private fun List<ResolvedEvent>.toStringList(): List<String> =
    map { e: ResolvedEvent -> String(e.originalEvent.eventData) }.toList()

private fun EventStoreDBClient.readStreamFromStart(streamName: String) =
    readStream(streamName, ReadStreamOptions.get().fromStart())
