package mastermind.journal

sealed interface JournalFailure<FAILURE> {
    sealed interface EventStoreFailure : JournalFailure<Nothing> {
        data class StreamNotFound(val streamName: StreamName) : EventStoreFailure
        data class ExecutionFailure<FAILURE>(val cause: FAILURE) : JournalFailure<FAILURE>
    }
}