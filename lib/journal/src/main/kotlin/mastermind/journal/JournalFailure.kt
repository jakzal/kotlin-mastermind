package mastermind.journal

sealed interface JournalFailure<FAILURE> {
    sealed interface EventStoreFailure<FAILURE> : JournalFailure<FAILURE> {
        data class StreamNotFound<FAILURE>(val streamName: StreamName) : EventStoreFailure<FAILURE>
        // @TODO include the expected and actual versions
        data class VersionConflict<FAILURE>(val streamName: StreamName) : EventStoreFailure<FAILURE>
    }

    data class ExecutionFailure<FAILURE>(val cause: FAILURE) : JournalFailure<FAILURE>
}