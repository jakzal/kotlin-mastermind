package mastermind.eventstore

import arrow.atomic.Atomic
import arrow.core.Either
import arrow.core.NonEmptyList
import arrow.core.getOrElse
import arrow.core.raise.either
import arrow.core.toNonEmptyListOrNone
import mastermind.eventstore.EventStoreError.StreamNotFound
import mastermind.eventstore.Stream.LoadedStream
import mastermind.eventstore.Stream.UpdatedStream

class InMemoryEventStore<EVENT : Any, ERROR : Any>(
    private val events: Atomic<Map<StreamName, LoadedStream<EVENT>>> = Atomic(mapOf())
) : EventStore<EVENT, ERROR> {
    override suspend fun load(streamName: StreamName): Either<EventStoreError<ERROR>, LoadedStream<EVENT>> = either {
        events.get()[streamName] ?: raise(StreamNotFound(streamName))
    }

    override suspend fun append(stream: UpdatedStream<EVENT>): Either<EventStoreError<ERROR>, LoadedStream<EVENT>> =
        either {
            events.updateAndGet {
                it[stream.streamName]?.let { writtenStream ->
                    if (writtenStream.streamVersion != stream.streamVersion) {
                        raise(
                            EventStoreError.VersionConflict(
                                stream.streamName,
                                stream.streamVersion,
                                writtenStream.streamVersion
                            )
                        )
                    }
                }
                it + mapOf(stream.streamName to stream.toLoadedStream())
            }[stream.streamName] ?: raise(StreamNotFound(stream.streamName))
        }
}

private fun <EVENT : Any> UpdatedStream<EVENT>.toLoadedStream(): LoadedStream<EVENT> =
    LoadedStream(streamName, streamVersion + eventsToAppend.size, events + eventsToAppend)

private infix operator fun <T> List<T>.plus(other: NonEmptyList<T>): NonEmptyList<T> =
    toNonEmptyListOrNone()
        .map { items -> items + other }
        .getOrElse { other }
