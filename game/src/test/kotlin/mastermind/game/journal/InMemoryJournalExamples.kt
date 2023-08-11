package mastermind.game.journal

class InMemoryJournalExamples : JournalContract() {
    private val journal = InMemoryJournal<TestEvent>()
    override fun journal(): Journal<TestEvent> = journal
    override fun loadEvents(streamName: StreamName): List<TestEvent> = journal.eventsFor(streamName)
}