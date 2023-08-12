package mastermind.game

import mastermind.game.journal.InMemoryJournal
import mastermind.game.journal.Journal
import mastermind.game.journal.JournalCommandHandler

data class Configuration(
    val gameIdGenerator: GameIdGenerator = GameIdGenerator { generateGameId() },
    val codeMaker: CodeMaker = CodeMaker { makeCode() },
    val journalCommandHandler: JournalCommandHandler<GameCommand, GameEvent, GameFailure, GameId> = JournalCommandHandler(
        ::execute,
        { command -> "Mastermind:${command.gameId.value}" },
        { events -> events.head.gameId }
    ),
    val journal: Journal<GameEvent> = InMemoryJournal(),
    val gameCommandHandler: GameCommandHandler = GameCommandHandler {
        with(journal) {
            // @TODO make GameCommandHandler play nice with failures
            journalCommandHandler(it).getOrNull()!!
        }
    }
) : GameIdGenerator by gameIdGenerator,
    CodeMaker by codeMaker,
    GameCommandHandler by gameCommandHandler

data class MastermindApp(
    val configuration: Configuration = Configuration(),
    val joinGame: suspend () -> GameId = {
        with(configuration) {
            mastermind.game.joinGame()
        }
    },
    val viewDecodingBoard: suspend (GameId) -> DecodingBoard? = { TODO() }
)
