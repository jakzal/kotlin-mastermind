package mastermind.eventstore

sealed interface EventStoreError {
    data class StreamNotFound(val streamName: StreamName) : EventStoreError
    data class VersionConflict(
        val streamName: StreamName,
        val expectedVersion: StreamVersion,
        val actualVersion: StreamVersion
    ) : EventStoreError
}