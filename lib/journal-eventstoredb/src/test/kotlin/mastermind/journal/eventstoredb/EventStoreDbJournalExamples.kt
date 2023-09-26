package mastermind.journal.eventstoredb

import com.eventstore.dbclient.*
import mastermind.journal.*
import mastermind.testkit.testcontainers.eventstoredb.EventStoreDbContainer
import org.junit.jupiter.api.Tag
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@Testcontainers(disabledWithoutDocker = true)
@Tag("io")
class EventStoreDbJournalExamples : JournalContract() {
    private val journal = EventStoreDbJournal<TestEvent, TestFailure>(EventStoreDb(eventStoreClient()))

    companion object {
        @Container
        private val eventStoreDb: EventStoreDbContainer<*> = EventStoreDbContainer()
    }

    override fun journal(): Journal<TestEvent, TestFailure> = journal

    override suspend fun loadEvents(streamName: StreamName): List<TestEvent> =
        journal.load(streamName).fold({ emptyList() }, { it.events })

    private fun eventStoreClient(): EventStoreDBClient = EventStoreDBClient.create(
        EventStoreDBConnectionString.parseOrThrow(eventStoreDb.connectionString)
    )
}
