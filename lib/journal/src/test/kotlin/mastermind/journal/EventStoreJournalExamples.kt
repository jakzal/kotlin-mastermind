package mastermind.journal

class EventStoreJournalExamples : JournalContract() {
    private val journal = EventStoreJournal<TestEvent, TestFailure>(InMemoryEventStore())
    override fun journal(): Journal<TestEvent, TestFailure> = journal
    override suspend fun loadEvents(streamName: StreamName): List<TestEvent> =
        journal.load(streamName).fold(
            { emptyList() },
            { it.events }
        )
}