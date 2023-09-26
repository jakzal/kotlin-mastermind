package mastermind.journal

import arrow.atomic.Atomic
import arrow.core.*
import arrow.core.raise.either
import mastermind.journal.JournalFailure.EventStoreFailure
import mastermind.journal.JournalFailure.EventStoreFailure.StreamNotFound
import mastermind.journal.JournalFailure.EventStoreFailure.VersionConflict
import mastermind.journal.JournalFailure.ExecutionFailure
import mastermind.journal.Stream.*


class InMemoryEventStore<EVENT : Any, FAILURE : Any>(
    private val events: Atomic<Map<StreamName, LoadedStream<EVENT>>> = Atomic(mapOf())
) : EventStore<EVENT, FAILURE> {
    override fun load(streamName: StreamName): Either<EventStoreFailure<FAILURE>, LoadedStream<EVENT>> =
        events.get()[streamName]?.right() ?: StreamNotFound<FAILURE>(streamName).left()

    override fun append(stream: UpdatedStream<EVENT>): Either<EventStoreFailure<FAILURE>, LoadedStream<EVENT>> =
        either {
            events.updateAndGet {
                it[stream.streamName]?.let { writtenStream ->
                    if (writtenStream.streamVersion != stream.streamVersion) {
                        raise(
                            VersionConflict(
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

class InMemoryJournal<EVENT : Any, FAILURE : Any>(
    private val eventStore: InMemoryEventStore<EVENT, FAILURE> = InMemoryEventStore()
) : Journal<EVENT, FAILURE> {

    override suspend fun stream(
        streamName: StreamName,
        onStream: Stream<EVENT>.() -> Either<FAILURE, UpdatedStream<EVENT>>
    ): Either<JournalFailure<FAILURE>, LoadedStream<EVENT>> =
        load(streamName)
            .orCreate(streamName)
            .execute(onStream)
            .append()

    override suspend fun load(streamName: StreamName): Either<EventStoreFailure<FAILURE>, LoadedStream<EVENT>> =
        (eventStore::load)(streamName)

    private fun Either<EventStoreFailure<FAILURE>, LoadedStream<EVENT>>.orCreate(streamName: StreamName): Either<JournalFailure<FAILURE>, Stream<EVENT>> =
        recover<JournalFailure<FAILURE>, JournalFailure<FAILURE>, Stream<EVENT>> { e ->
            if (e is StreamNotFound) EmptyStream(streamName)
            else raise(e)
        }

    private fun Either<JournalFailure<FAILURE>, Stream<EVENT>>.execute(onStream: Stream<EVENT>.() -> Either<FAILURE, UpdatedStream<EVENT>>): Either<JournalFailure<FAILURE>, UpdatedStream<EVENT>> =
        flatMap { stream -> stream.onStream().mapLeft(::ExecutionFailure) }

    private fun Either<JournalFailure<FAILURE>, UpdatedStream<EVENT>>.append(): Either<JournalFailure<FAILURE>, LoadedStream<EVENT>> =
        flatMap { streamToWrite -> (eventStore::append)(streamToWrite) }
}

private fun <EVENT : Any> UpdatedStream<EVENT>.toLoadedStream(): LoadedStream<EVENT> =
    LoadedStream(streamName, streamVersion + eventsToAppend.size, events + eventsToAppend)

private infix operator fun <T> List<T>.plus(other: NonEmptyList<T>): NonEmptyList<T> =
    toNonEmptyListOrNone()
        .map { items -> items + other }
        .getOrElse { other }
