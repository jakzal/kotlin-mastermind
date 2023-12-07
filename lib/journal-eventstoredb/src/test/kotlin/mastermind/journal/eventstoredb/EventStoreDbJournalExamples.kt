package mastermind.journal.eventstoredb

import mastermind.journal.JournalContract
import mastermind.testkit.testcontainers.eventstoredb.EventStoreDbContainer
import org.junit.jupiter.api.Tag
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@Testcontainers(disabledWithoutDocker = true)
@Tag("io")
class EventStoreDbJournalExamples : JournalContract(EventStoreDbJournal(eventStoreDb.connectionString)) {
    companion object {
        @Container
        private val eventStoreDb: EventStoreDbContainer<*> = EventStoreDbContainer()
    }
}
