package mastermind.journal

import arrow.atomic.Atomic

class InMemoryEventStoreExamples : EventStoreContract(
    InMemoryEventStore(Atomic(mapOf()))
)

