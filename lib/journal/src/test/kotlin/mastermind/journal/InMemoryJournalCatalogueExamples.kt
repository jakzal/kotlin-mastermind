package mastermind.journal

import arrow.atomic.Atomic

class InMemoryJournalCatalogueExamples : JournalCatalogueContract(
    InMemoryJournalCatalogue(Atomic(mapOf()))
)

