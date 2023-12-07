package mastermind.journal

sealed interface JournalError<out ERROR> {
    data class ExecutionError<ERROR>(val cause: ERROR) : JournalError<ERROR>
    data class StreamNotFound(val streamName: StreamName) : JournalError<Nothing>
    data class VersionConflict(
        val streamName: StreamName,
        val expectedVersion: StreamVersion,
        val actualVersion: StreamVersion
    ) : JournalError<Nothing>
}