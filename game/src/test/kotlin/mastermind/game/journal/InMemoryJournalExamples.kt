package mastermind.game.journal

class InMemoryJournalExamples : JournalContract() {
    override fun createJournal(): Journal<TestEvent> = InMemoryJournal()
}