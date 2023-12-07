package mastermind.eventstore

import arrow.atomic.Atomic

class InMemoryEventStoreExamples : EventStoreContract(
    InMemoryEventStore(Atomic(mapOf()))
)

