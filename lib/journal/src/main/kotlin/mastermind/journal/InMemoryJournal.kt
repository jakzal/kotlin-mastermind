package mastermind.journal

import arrow.atomic.Atomic
import arrow.core.*
import arrow.core.raise.either
import mastermind.journal.JournalFailure.StreamNotFound
import mastermind.journal.Stream.LoadedStream
import mastermind.journal.Stream.UpdatedStream

class InMemoryJournal<EVENT : Any, FAILURE : Any>(
    private val events: Atomic<Map<StreamName, LoadedStream<EVENT>>> = Atomic(mapOf())
) : Journal<EVENT, FAILURE> {
    override suspend fun load(streamName: StreamName): Either<JournalFailure<FAILURE>, LoadedStream<EVENT>> =
        events.get()[streamName]?.right() ?: StreamNotFound<FAILURE>(streamName).left()

    override suspend fun append(stream: UpdatedStream<EVENT>): Either<JournalFailure<FAILURE>, LoadedStream<EVENT>> =
        either {
            events.updateAndGet {
                it[stream.streamName]?.let { writtenStream ->
                    if (writtenStream.streamVersion != stream.streamVersion) {
                        raise(
                            JournalFailure.VersionConflict(
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
