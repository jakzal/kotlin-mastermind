package mastermind.eventstore.eventstoredb

import mastermind.eventstore.EventStoreContract
import mastermind.testkit.testcontainers.eventstoredb.EventStoreDbContainer
import org.junit.jupiter.api.Tag
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@Testcontainers(disabledWithoutDocker = true)
@Tag("io")
class EventStoreDbEventStoreExamples : EventStoreContract(EventStoreDbEventStore(eventStoreDb.connectionString)) {
    companion object {
        @Container
        private val eventStoreDb: EventStoreDbContainer<*> = EventStoreDbContainer()
    }
}
