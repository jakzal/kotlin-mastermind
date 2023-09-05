package mastermind.journal

import arrow.atomic.Atomic
import arrow.atomic.update
import arrow.core.*
import arrow.core.raise.either
import mastermind.journal.JournalFailure.EventStoreFailure
import mastermind.journal.JournalFailure.EventStoreFailure.StreamNotFound
import mastermind.journal.JournalFailure.EventStoreFailure.VersionConflict
import mastermind.journal.JournalFailure.ExecutionFailure
import mastermind.journal.Stream.*

class InMemoryJournal<EVENT : Any> : Journal<EVENT> {
    private val events = Atomic(mapOf<StreamName, LoadedStream<EVENT>>())

    override suspend fun <FAILURE : Any> stream(
        streamName: StreamName,
        onStream: Stream<EVENT>.() -> Either<FAILURE, UpdatedStream<EVENT>>
    ): Either<JournalFailure<FAILURE>, LoadedStream<EVENT>> =
        stream(streamName)
            .onStream()
            .map { stream -> stream.toLoadedStream() }
            .mapLeft { ExecutionFailure(it) }
            .onRight { streamToWrite ->
                events.update {
                    it[streamName]?.let { writtenStream ->
                        if (writtenStream.streamVersion >= streamToWrite.streamVersion) {
                            return VersionConflict<FAILURE>(streamName).left()
                        }
                    }
                    it + mapOf(streamName to streamToWrite)
                }
            }

    override suspend fun load(streamName: StreamName): Either<EventStoreFailure<Nothing>, LoadedStream<EVENT>> = either {
        events.get()[streamName] ?: raise(StreamNotFound(streamName))
    }

    private fun stream(streamName: StreamName) = events.get()[streamName] ?: EmptyStream(streamName)
}

private fun <EVENT : Any> UpdatedStream<EVENT>.toLoadedStream(): LoadedStream<EVENT> =
    LoadedStream(streamName, streamVersion + eventsToAppend.size, events.append(eventsToAppend))

private fun <EVENT : Any> List<EVENT>.append(eventsToAppend: NonEmptyList<EVENT>): NonEmptyList<EVENT> =
    toNonEmptyListOrNone()
        .map { events -> events + eventsToAppend }
        .getOrElse { eventsToAppend }
