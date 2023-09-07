package mastermind.journal.eventstoredb

import arrow.core.Either
import com.eventstore.dbclient.EventStoreDBClient
import com.eventstore.dbclient.EventStoreDBConnectionString
import mastermind.journal.*
import mastermind.testkit.testcontainers.eventstoredb.EventStoreDbContainer
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Tag
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@Testcontainers(disabledWithoutDocker = true)
@Tag("io")
@Disabled
class EventStoreDbJournalExamples : JournalContract() {
    private val journal = EventStoreDbJournal<TestEvent, TestFailure>(eventStoreClient())

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

class EventStoreDbJournal<EVENT : Any, FAILURE : Any>(eventStore: EventStoreDBClient) : Journal<EVENT, FAILURE> {
    override suspend fun <FAILURE : Any> stream(
        streamName: StreamName,
        onStream: Stream<EVENT>.() -> Either<FAILURE, Stream.UpdatedStream<EVENT>>
    ): Either<JournalFailure<FAILURE>, Stream.LoadedStream<EVENT>> {
        TODO("Not yet implemented")
    }

    override suspend fun load(streamName: StreamName): Either<JournalFailure.EventStoreFailure<FAILURE>, Stream.LoadedStream<EVENT>> {
        TODO("Not yet implemented")
    }
}
