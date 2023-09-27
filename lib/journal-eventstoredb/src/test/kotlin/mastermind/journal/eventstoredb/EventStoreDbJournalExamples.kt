package mastermind.journal.eventstoredb

import com.eventstore.dbclient.EventStoreDBClient
import com.eventstore.dbclient.EventStoreDBConnectionString
import mastermind.journal.JournalContract
import mastermind.testkit.testcontainers.eventstoredb.EventStoreDbContainer
import org.junit.jupiter.api.Tag
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@Testcontainers(disabledWithoutDocker = true)
@Tag("io")
class EventStoreDbJournalExamples : JournalContract(EventStoreDbJournal(eventStoreClient())) {
    companion object {
        @Container
        private val eventStoreDb: EventStoreDbContainer<*> = EventStoreDbContainer()

        private fun eventStoreClient(): EventStoreDBClient = EventStoreDBClient.create(
            EventStoreDBConnectionString.parseOrThrow(eventStoreDb.connectionString)
        )
    }
}
