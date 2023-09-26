package mastermind.journal

import arrow.atomic.Atomic
import arrow.core.*
import arrow.core.raise.either
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
    val append: (UpdatedStream<EVENT>) -> Either<JournalFailure<FAILURE>, LoadedStream<EVENT>> =
        { streamToWrite ->
            either {
                events.updateAndGet {
                    it[streamToWrite.streamName]?.let { writtenStream ->
                        if (writtenStream.streamVersion != streamToWrite.streamVersion) {
                            raise(
                                VersionConflict(
                                    streamToWrite.streamName,
                                    streamToWrite.streamVersion,
                                    writtenStream.streamVersion
                                )
                            )
                        }
                    }
                    it + mapOf(streamToWrite.streamName to streamToWrite.toLoadedStream())
                }[streamToWrite.streamName] ?: raise(StreamNotFound(streamToWrite.streamName))
            }
        }

    override suspend fun stream(
        streamName: StreamName,
        onStream: Stream<EVENT>.() -> Either<FAILURE, UpdatedStream<EVENT>>
    ): Either<JournalFailure<FAILURE>, LoadedStream<EVENT>> =
        load(streamName)
            .orCreate(streamName)
            .execute(onStream)
            .append()

    override suspend fun load(streamName: StreamName): Either<EventStoreFailure<FAILURE>, LoadedStream<EVENT>> =
        loadStream(streamName)

    private fun Either<EventStoreFailure<FAILURE>, LoadedStream<EVENT>>.orCreate(streamName: StreamName): Either<JournalFailure<FAILURE>, Stream<EVENT>> =
        recover<JournalFailure<FAILURE>, JournalFailure<FAILURE>, Stream<EVENT>> { e ->
            if (e is StreamNotFound) EmptyStream(streamName)
            else raise(e)
        }

    private fun Either<JournalFailure<FAILURE>, Stream<EVENT>>.execute(onStream: Stream<EVENT>.() -> Either<FAILURE, UpdatedStream<EVENT>>): Either<JournalFailure<FAILURE>, UpdatedStream<EVENT>> =
        flatMap { stream -> stream.onStream().mapLeft(::ExecutionFailure) }

    private fun Either<JournalFailure<FAILURE>, UpdatedStream<EVENT>>.append(): Either<JournalFailure<FAILURE>, LoadedStream<EVENT>> =
        flatMap { streamToWrite -> append(streamToWrite) }
}

private fun <EVENT : Any> UpdatedStream<EVENT>.toLoadedStream(): LoadedStream<EVENT> =
    LoadedStream(streamName, streamVersion + eventsToAppend.size, events + eventsToAppend)

private infix operator fun <T> List<T>.plus(other: NonEmptyList<T>): NonEmptyList<T> =
    toNonEmptyListOrNone()
        .map { items -> items + other }
        .getOrElse { other }
