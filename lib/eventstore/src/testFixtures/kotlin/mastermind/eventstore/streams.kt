package mastermind.eventstore

import arrow.core.NonEmptyList
import arrow.core.nonEmptyListOf
import mastermind.eventstore.Stream.*

fun <EVENT : Any> emptyStream(streamName: StreamName): EmptyStream<EVENT> = EmptyStream(streamName)

fun <EVENT : Any> loadedStream(streamName: String, events: NonEmptyList<EVENT>): LoadedStream<EVENT> =
    LoadedStream(streamName, events.size.toLong(), events)

fun <EVENT : Any> loadedStream(streamName: String, event: EVENT, vararg events: EVENT): LoadedStream<EVENT> =
    loadedStream(streamName, nonEmptyListOf(event, *events))

fun <EVENT : Any> updatedStream(
    streamName: StreamName,
    event: EVENT,
    vararg events: EVENT
): UpdatedStream<EVENT> =
    updatedStream(emptyStream(streamName), event, *events)

fun <EVENT : Any> updatedStream(
    existingStream: Stream<EVENT>,
    event: EVENT,
    vararg events: EVENT
): UpdatedStream<EVENT> =
    existingStream.append(event, *events)
