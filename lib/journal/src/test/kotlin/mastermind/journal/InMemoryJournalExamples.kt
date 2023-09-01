package mastermind.journal

class InMemoryJournalExamples : JournalContract() {
    private val journal = InMemoryJournal<TestEvent>()
    override fun journal(): Journal<TestEvent> = journal
    override suspend fun loadEvents(streamName: StreamName): List<TestEvent> =
        journal.load(streamName).fold(
            { emptyList() },
            { it.events }
        )
}