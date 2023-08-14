package mastermind.game.journal

import arrow.core.getOrElse

class InMemoryJournalExamples : JournalContract() {
    private val journal = InMemoryJournal<TestEvent>()
    override fun journal(): Journal<TestEvent> = journal
    override suspend fun loadEvents(streamName: StreamName): List<TestEvent> =
        journal.load(streamName).getOrElse { emptyList() }
}