package mastermind.journal

import arrow.atomic.Atomic
import arrow.atomic.update
import arrow.core.*
import mastermind.journal.JournalFailure.EventStoreFailure
import mastermind.journal.JournalFailure.EventStoreFailure.StreamNotFound
import mastermind.journal.JournalFailure.EventStoreFailure.VersionConflict
import mastermind.journal.JournalFailure.ExecutionFailure
import mastermind.journal.Stream.*

class InMemoryJournal<EVENT : Any, FAILURE : Any> : Journal<EVENT, FAILURE> {
    private val events = Atomic(mapOf<StreamName, LoadedStream<EVENT>>())
    val loadStream: (StreamName) -> Either<EventStoreFailure<FAILURE>, LoadedStream<EVENT>> =
        { streamName: StreamName ->
            events.get()[streamName]?.right() ?: StreamNotFound<FAILURE>(streamName).left()
        }

    override suspend fun stream(
        streamName: StreamName,
        onStream: Stream<EVENT>.() -> Either<FAILURE, UpdatedStream<EVENT>>
    ): Either<JournalFailure<FAILURE>, LoadedStream<EVENT>> =
        stream(streamName)
            .execute(onStream)
            .appendTo(streamName)

    override suspend fun load(streamName: StreamName): Either<EventStoreFailure<FAILURE>, LoadedStream<EVENT>> =
        loadStream(streamName)

    private fun stream(streamName: StreamName) = events.get()[streamName] ?: EmptyStream(streamName)

    private fun Either<JournalFailure<FAILURE>, UpdatedStream<EVENT>>.appendTo(streamName: StreamName): Either<JournalFailure<FAILURE>, LoadedStream<EVENT>> =
        onRight { streamToWrite ->
            events.update {
                it[streamName]?.let { writtenStream ->
                    if (writtenStream.streamVersion != streamToWrite.streamVersion) {
                        return VersionConflict<FAILURE>(
                            streamName,
                            streamToWrite.streamVersion,
                            writtenStream.streamVersion
                        ).left()
                    }
                }
                it + mapOf(streamName to streamToWrite.toLoadedStream())
            }
        }.map(UpdatedStream<EVENT>::toLoadedStream)
}

private fun <EVENT : Any, FAILURE : Any> Stream<EVENT>.execute(onStream: Stream<EVENT>.() -> Either<FAILURE, UpdatedStream<EVENT>>) =
    onStream().mapLeft { ExecutionFailure(it) }

private fun <EVENT : Any> UpdatedStream<EVENT>.toLoadedStream(): LoadedStream<EVENT> =
    LoadedStream(streamName, streamVersion + eventsToAppend.size, events + eventsToAppend)

private infix operator fun <T> List<T>.plus(other: NonEmptyList<T>): NonEmptyList<T> =
    toNonEmptyListOrNone()
        .map { items -> items + other }
        .getOrElse { other }
