package mastermind.journal

sealed interface JournalError<out ERROR> {
    data class ExecutionError<ERROR>(val cause: ERROR) : JournalError<ERROR>
    data class StreamNotFound(val journalName: JournalName) : JournalError<Nothing>
    data class VersionConflict(
        val journalName: JournalName,
        val expectedVersion: JournalVersion,
        val actualVersion: JournalVersion
    ) : JournalError<Nothing>
}