package mastermind.journal

import arrow.core.NonEmptyList

typealias StreamName = String
typealias StreamVersion = Long

sealed interface Stream<EVENT : Any> {
    val streamName: StreamName
    val streamVersion: StreamVersion
    val events: List<EVENT>

    data class EmptyStream<EVENT : Any>(override val streamName: StreamName) : Stream<EVENT> {
        override val streamVersion: StreamVersion
            get() = 0
        override val events: List<EVENT>
            get() = emptyList()
    }

    data class LoadedStream<EVENT : Any>(
        override val streamName: StreamName,
        override val streamVersion: StreamVersion,
        override val events: NonEmptyList<EVENT>
    ) : Stream<EVENT>

    data class UpdatedStream<EVENT : Any>(
        override val streamName: StreamName,
        override val streamVersion: StreamVersion,
        override val events: List<EVENT>,
        val eventsToAppend: NonEmptyList<EVENT>
    ) : Stream<EVENT>
}