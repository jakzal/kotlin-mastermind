package mastermind.journal

import mastermind.journal.Stream.EmptyStream
import mastermind.journal.Stream.UpdatedStream

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
