package mastermind.journal

sealed interface JournalError<ERROR> {
    data class ExecutionError<ERROR>(val cause: ERROR) : JournalError<ERROR>
    data class StreamNotFound<ERROR>(val streamName: StreamName) : JournalError<ERROR>
    data class VersionConflict<ERROR>(
        val streamName: StreamName,
        val expectedVersion: StreamVersion,
        val actualVersion: StreamVersion
    ) : JournalError<ERROR>
}