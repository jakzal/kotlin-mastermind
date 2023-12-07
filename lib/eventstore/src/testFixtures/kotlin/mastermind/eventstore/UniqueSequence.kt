package mastermind.eventstore

import arrow.atomic.AtomicInt

class UniqueSequence<T>(
    private val nextItem: (Int) -> T
) {
    companion object {
        private val streamCount = AtomicInt(0)
    }

    operator fun invoke(): T = nextItem(streamCount.incrementAndGet())
}