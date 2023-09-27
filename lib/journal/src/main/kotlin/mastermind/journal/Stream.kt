package mastermind.journal

import arrow.core.*
import mastermind.journal.Stream.LoadedStream
import mastermind.journal.Stream.UpdatedStream

typealias StreamName = String
typealias StreamVersion = Long

typealias OnStream<EVENT, FAILURE> = Stream<EVENT>.() -> Either<FAILURE, UpdatedStream<EVENT>>
typealias UpdateStream<EVENT, FAILURE> =
        suspend (StreamName, OnStream<EVENT, FAILURE>) -> Either<JournalFailure<FAILURE>, LoadedStream<EVENT>>
typealias LoadStream<EVENT, FAILURE> = suspend (StreamName) -> Either<JournalFailure<FAILURE>, LoadedStream<EVENT>>

sealed interface Stream<EVENT : Any> {
    val streamName: StreamName
    val streamVersion: StreamVersion
    val events: List<EVENT>

    data class EmptyStream<EVENT : Any>(override val streamName: StreamName) : Stream<EVENT> {
        override val streamVersion: StreamVersion = 0
        override val events: List<EVENT> = emptyList()
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

fun <EVENT : Any, ERROR : Any> Stream<EVENT>.append(
    generateEvents: () -> Either<ERROR, NonEmptyList<EVENT>>
): Either<ERROR, UpdatedStream<EVENT>> =
    generateEvents().flatMap(::append)

fun <EVENT : Any, ERROR : Any> Stream<EVENT>.append(
    event: EVENT,
    vararg events: EVENT
): Either<ERROR, UpdatedStream<EVENT>> =
    append(nonEmptyListOf(event, *events))

private fun <EVENT : Any, ERROR : Any> Stream<EVENT>.append(eventsToAppend: NonEmptyList<EVENT>): Either<ERROR, UpdatedStream<EVENT>> =
    UpdatedStream(streamName, streamVersion, events, eventsToAppend).right()