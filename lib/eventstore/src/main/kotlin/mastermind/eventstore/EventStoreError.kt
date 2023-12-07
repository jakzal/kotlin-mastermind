package mastermind.eventstore

sealed interface EventStoreError<out ERROR> {
    data class ExecutionError<ERROR>(val cause: ERROR) : EventStoreError<ERROR>
    data class StreamNotFound(val streamName: StreamName) : EventStoreError<Nothing>
    data class VersionConflict(
        val streamName: StreamName,
        val expectedVersion: StreamVersion,
        val actualVersion: StreamVersion
    ) : EventStoreError<Nothing>
}