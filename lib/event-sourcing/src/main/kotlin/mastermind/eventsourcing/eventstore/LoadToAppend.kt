package mastermind.eventsourcing.eventstore

import arrow.core.*
import mastermind.eventstore.*
import mastermind.eventstore.EventStoreError.StreamNotFound
import mastermind.eventstore.Stream.*

sealed interface EventSourcingError<out ERROR : Any> {
    data class ExecutionError<ERROR : Any>(val cause: ERROR) : EventSourcingError<ERROR>
    data class EventStoreError(val cause: mastermind.eventstore.EventStoreError) : EventSourcingError<Nothing>
}

context(EventStore<EVENT>)
suspend fun <EVENT : Any, ERROR : Any> loadToAppend(
    streamName: StreamName,
    onEvents: (List<EVENT>) -> Either<ERROR, NonEmptyList<EVENT>>
): Either<EventSourcingError<ERROR>, NonEmptyList<EVENT>> =
    load(streamName)
        .orCreate(streamName)
        .update(onEvents)
        .persist()
        .map(LoadedStream<EVENT>::events)

private fun <EVENT : Any> Either<EventStoreError, LoadedStream<EVENT>>.orCreate(
    streamName: StreamName
): Either<EventStoreError, Stream<EVENT>> =
    recover { e ->
        if (e is StreamNotFound) EmptyStream(streamName)
        else raise(e)
    }

private fun <EVENT : Any, ERROR : Any> Either<EventStoreError, Stream<EVENT>>.update(
    onEvents: (List<EVENT>) -> Either<ERROR, NonEmptyList<EVENT>>
): Either<EventSourcingError<ERROR>, UpdatedStream<EVENT>> =
    mapLeft { EventSourcingError.EventStoreError(it) }
        .flatMap { stream ->
            stream.append {
                onEvents(stream.events)
                    .mapLeft { EventSourcingError.ExecutionError(it) }
            }
        }


context(EventStore<EVENT>)
private suspend fun <EVENT : Any, ERROR : Any> Either<EventSourcingError<ERROR>, UpdatedStream<EVENT>>.persist()
        : Either<EventSourcingError<ERROR>, LoadedStream<EVENT>> =
    flatMap { streamToWrite ->
        append(streamToWrite)
            .mapLeft { EventSourcingError.EventStoreError(it) }
    }

