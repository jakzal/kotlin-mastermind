package mastermind.journal

class InMemoryJournalExamples : JournalContract() {
    private val journal = InMemoryJournal<TestEvent, TestFailure>()
    override fun journal(): Journal<TestEvent, TestFailure> = journal
    override suspend fun loadEvents(streamName: StreamName): List<TestEvent> =
        journal.load(streamName).fold(
            { emptyList() },
            { it.events }
        )
}