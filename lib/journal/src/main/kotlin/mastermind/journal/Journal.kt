package mastermind.journal

import arrow.core.*
import mastermind.journal.JournalFailure.EventStoreFailure
import mastermind.journal.Stream.LoadedStream
import mastermind.journal.Stream.UpdatedStream

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

interface Journal<EVENT : Any> {
    suspend fun <FAILURE : Any> stream(
        streamName: StreamName,
        execute: Stream<EVENT>.() -> Either<FAILURE, UpdatedStream<EVENT>>
    ): Either<JournalFailure<FAILURE>, LoadedStream<EVENT>>

    suspend fun load(streamName: StreamName): Either<EventStoreFailure, LoadedStream<EVENT>>
}

sealed interface JournalFailure<FAILURE> {
    sealed interface EventStoreFailure : JournalFailure<Nothing> {
        data class StreamNotFound(val streamName: StreamName) : EventStoreFailure
        data class ExecutionFailure<FAILURE>(val cause: FAILURE) : JournalFailure<FAILURE>
    }
}

fun <EVENT : Any, ERROR : Any> Stream<EVENT>.append(generateEvents: () -> Either<ERROR, NonEmptyList<EVENT>>): Either<ERROR, UpdatedStream<EVENT>> =
    generateEvents().flatMap { append(it) }

fun <EVENT : Any, ERROR : Any> Stream<EVENT>.append(
    event: EVENT,
    vararg events: EVENT
): Either<ERROR, UpdatedStream<EVENT>> =
    append(nonEmptyListOf(event, *events))

fun <EVENT : Any, ERROR : Any> Stream<EVENT>.append(eventsToAppend: NonEmptyList<EVENT>): Either<ERROR, UpdatedStream<EVENT>> =
    UpdatedStream(streamName, streamVersion, events, eventsToAppend).right()

fun <EVENT : Any> UpdatedStream<EVENT>.toLoadedStream(): LoadedStream<EVENT> {
    val mergedEvents =
        if (this.events.isEmpty()) this.eventsToAppend
        else nonEmptyListOf(this.events.first()) + this.events.tail() + this.eventsToAppend
    return LoadedStream(streamName, this.streamVersion + this.eventsToAppend.size, mergedEvents)
}
