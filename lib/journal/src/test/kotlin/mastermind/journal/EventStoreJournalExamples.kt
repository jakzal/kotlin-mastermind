package mastermind.journal

class EventStoreJournalExamples : JournalContract() {
    private val journal = with(InMemoryEventStore<TestEvent, TestFailure>()) {
        EventStoreJournal()
    }
    override fun journal(): Journal<TestEvent, TestFailure> = journal
    override suspend fun loadEvents(streamName: StreamName): List<TestEvent> =
        journal.load(streamName).fold(
            { emptyList() },
            { it.events }
        )
}