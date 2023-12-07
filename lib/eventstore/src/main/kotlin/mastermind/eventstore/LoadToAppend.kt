package mastermind.eventstore

import arrow.core.Either
import arrow.core.NonEmptyList
import arrow.core.flatMap
import arrow.core.recover
import mastermind.eventstore.EventStoreError.ExecutionError
import mastermind.eventstore.EventStoreError.StreamNotFound
import mastermind.eventstore.Stream.*

private typealias LoadedStreamOutcome<ERROR, EVENT> = Either<EventStoreError<ERROR>, LoadedStream<EVENT>>
private typealias StreamOutcome<ERROR, EVENT> = Either<EventStoreError<ERROR>, Stream<EVENT>>
private typealias UpdatedStreamOutcome<ERROR, EVENT> = Either<EventStoreError<ERROR>, UpdatedStream<EVENT>>

context(EventStore<EVENT, ERROR>)
suspend fun <EVENT : Any, ERROR : Any> loadToAppend(
    streamName: StreamName,
    onEvents: (List<EVENT>) -> Either<ERROR, NonEmptyList<EVENT>>
): Either<EventStoreError<ERROR>, NonEmptyList<EVENT>> =
    load(streamName)
        .orCreate(streamName)
        .update(onEvents)
        .persist()
        .map(LoadedStream<EVENT>::events)

private fun <EVENT : Any, ERROR : Any> LoadedStreamOutcome<ERROR, EVENT>.orCreate(
    streamName: StreamName
): StreamOutcome<ERROR, EVENT> =
    recover { e ->
        if (e is StreamNotFound) EmptyStream(streamName)
        else raise(e)
    }

private fun <EVENT : Any, ERROR : Any> StreamOutcome<ERROR, EVENT>.update(
    onEvents: (List<EVENT>) -> Either<ERROR, NonEmptyList<EVENT>>
): UpdatedStreamOutcome<ERROR, EVENT> =
    flatMap { stream ->
        stream
            .append { onEvents(stream.events) }
            .mapLeft(::ExecutionError)
    }

context(EventStore<EVENT, ERROR>)
private suspend fun <EVENT : Any, ERROR : Any> UpdatedStreamOutcome<ERROR, EVENT>.persist()
        : LoadedStreamOutcome<ERROR, EVENT> =
    flatMap { streamToWrite -> append(streamToWrite) }
