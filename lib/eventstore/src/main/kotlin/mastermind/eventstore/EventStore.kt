package mastermind.eventstore

import arrow.core.Either
import mastermind.eventstore.Stream.LoadedStream
import mastermind.eventstore.Stream.UpdatedStream

interface EventStore<EVENT : Any, ERROR : Any> {
    suspend fun load(streamName: StreamName): Either<EventStoreError<ERROR>, LoadedStream<EVENT>>
    suspend fun append(stream: UpdatedStream<EVENT>): Either<EventStoreError<ERROR>, LoadedStream<EVENT>>
}