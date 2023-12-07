package mastermind.game.config

import mastermind.eventstore.InMemoryEventStore
import mastermind.eventstore.eventstoredb.EventStoreDbEventStore
import mastermind.game.EventStoreModule
import mastermind.game.GameError
import mastermind.game.GameEvent
import org.http4k.cloudnative.env.Environment
import org.http4k.cloudnative.env.EnvironmentKey
import org.http4k.lens.nonBlankString

fun Environment.asEventStoreModule(): EventStoreModule<GameEvent, GameError> =
    when (val eventStoreDbUrl = EnvironmentKey.nonBlankString().optional("eventstoredb.url")(this)) {
        null -> EventStoreModule(InMemoryEventStore())
        else -> EventStoreModule(EventStoreDbEventStore(eventStoreDbUrl))
    }
