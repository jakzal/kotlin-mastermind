package mastermind.journal

import arrow.atomic.Atomic

class InMemoryJournalExamples : JournalContract(
    InMemoryJournal(Atomic(mapOf()))
)

