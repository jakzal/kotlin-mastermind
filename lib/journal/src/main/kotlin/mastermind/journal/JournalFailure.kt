package mastermind.journal

sealed interface JournalFailure<FAILURE> {
    data class ExecutionFailure<FAILURE>(val cause: FAILURE) : JournalFailure<FAILURE>
    data class StreamNotFound<FAILURE>(val streamName: StreamName) : JournalFailure<FAILURE>
    data class VersionConflict<FAILURE>(
        val streamName: StreamName,
        val expectedVersion: StreamVersion,
        val actualVersion: StreamVersion
    ) : JournalFailure<FAILURE>
}