package mastermind.game.config

import mastermind.game.GameError
import mastermind.game.GameEvent
import mastermind.game.JournalModule
import mastermind.journal.InMemoryJournalCatalogue
import mastermind.journal.eventstoredb.EventStoreDbJournalCatalogue
import org.http4k.cloudnative.env.Environment
import org.http4k.cloudnative.env.EnvironmentKey
import org.http4k.lens.nonBlankString

fun Environment.asJournalModule(): JournalModule<GameEvent, GameError> =
    when (val eventStoreDbUrl = EnvironmentKey.nonBlankString().optional("eventstoredb.url")(this)) {
        null -> JournalModule(InMemoryJournalCatalogue())
        else -> JournalModule(EventStoreDbJournalCatalogue(eventStoreDbUrl))
    }
