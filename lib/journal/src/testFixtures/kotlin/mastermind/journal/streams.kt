package mastermind.journal

import arrow.core.NonEmptyList
import arrow.core.nonEmptyListOf
import mastermind.journal.Stream.*

fun <EVENT : Any> emptyStream(streamName: String): EmptyStream<EVENT> =
    EmptyStream(streamName)

fun <EVENT : Any> loadedStream(streamName: String, events: NonEmptyList<EVENT>): LoadedStream<EVENT> =
    LoadedStream(streamName, events.size.toLong(), events)

fun <EVENT : Any> loadedStream(streamName: String, event: EVENT, vararg events: EVENT): LoadedStream<EVENT> =
    loadedStream(streamName, nonEmptyListOf(event, *events))

fun <EVENT : Any> updatedStream(
    streamName: StreamName,
    event: EVENT,
    vararg events: EVENT
): UpdatedStream<EVENT> =
    updatedStream(EmptyStream(streamName), event, *events)

fun <EVENT : Any> updatedStream(
    existingStream: Stream<EVENT>,
    event: EVENT,
    vararg events: EVENT
): UpdatedStream<EVENT> =
    existingStream.append<EVENT, Nothing>(event, *events).getOrNull()
        ?: throw RuntimeException("Failed to create an updated stream.")
