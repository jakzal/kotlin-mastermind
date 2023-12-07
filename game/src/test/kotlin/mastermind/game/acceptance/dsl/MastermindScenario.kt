package mastermind.game.acceptance.dsl

import kotlinx.coroutines.test.runTest
import mastermind.game.*
import mastermind.game.acceptance.dsl.direct.DirectPlayGameAbility
import mastermind.game.acceptance.dsl.http.HttpPlayGameAbility
import mastermind.game.acceptance.dsl.junit.ExecutionContext
import mastermind.game.acceptance.dsl.junit.dynamicContainer
import mastermind.game.http.ServerRunnerModule
import mastermind.game.testkit.DirectRunnerModule
import mastermind.journal.InMemoryJournal
import mastermind.journal.eventstoredb.EventStoreDbJournal
import org.junit.jupiter.api.DynamicContainer
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.DynamicTest.dynamicTest

class MastermindScenario(
    private val ability: PlayGameAbility,
    val secret: Code,
    val totalAttempts: Int,
    val availablePegs: Set<Code.Peg>
) : PlayGameAbility by ability {
    companion object {
        context(ScenarioContext)
        operator fun invoke(
            secret: Code,
            totalAttempts: Int = 12,
            availablePegs: Set<Code.Peg> = setOfPegs("Red", "Green", "Blue", "Yellow", "Purple"),
            scenario: suspend MastermindScenario.() -> Unit
        ) {
            val app = MastermindApp(
                gameModule = GameModule(
                    makeCode = { secret },
                    totalAttempts = totalAttempts,
                    availablePegs = availablePegs,
                    journalModule = journalModule()
                ),
                runnerModule = runnerModule()
            )
            app.start()
            app.use {
                runTest {
                    MastermindScenario(app.playGameAbility(), secret, totalAttempts, availablePegs)
                        .scenario()
                }
            }
        }
    }
}

context(ExecutionContext)
fun mastermindScenario(
    secret: Code,
    totalAttempts: Int = 12,
    availablePegs: Set<Code.Peg> = setOfPegs("Red", "Green", "Blue", "Yellow", "Purple"),
    scenario: suspend MastermindScenario.() -> Unit
): List<DynamicTest> =
    scenarioContexts()
        .map { context ->
            dynamicTest(context.mode.name) {
                with(context) {
                    MastermindScenario(secret, totalAttempts, availablePegs, scenario)
                }
            }
        }

context(ExecutionContext)
fun mastermindScenarios(
    scenarios: List<Pair<String, context(ScenarioContext) () -> Unit>>
): List<DynamicContainer> =
    scenarios
        .map { (displayName, scenario) ->
            dynamicContainer(
                displayName,
                scenarioContexts().map { context ->
                    "${context.mode}" to {
                        with(context) {
                            scenario(this)
                        }
                    }
                }
            )
        }

data class ScenarioContext(val mode: ExecutionMode, val journal: Journal) {
    enum class ExecutionMode {
        HTTP,
        DIRECT
    }
    enum class Journal {
        IN_MEMORY,
        EVENT_STORE_DB
    }
}

private fun ScenarioContext.runnerModule(): RunnerModule = when (mode) {
    ScenarioContext.ExecutionMode.HTTP -> ServerRunnerModule(0)
    ScenarioContext.ExecutionMode.DIRECT -> DirectRunnerModule()
}


private fun ScenarioContext.journalModule(): JournalModule<GameEvent, GameError> = when(journal) {
    ScenarioContext.Journal.IN_MEMORY -> JournalModule(InMemoryJournal())
    ScenarioContext.Journal.EVENT_STORE_DB -> JournalModule(EventStoreDbJournal("esdb://localhost:2113?tls=false"))
}

private fun MastermindApp.playGameAbility(): PlayGameAbility = when (runnerModule) {
    is ServerRunnerModule -> HttpPlayGameAbility((runnerModule as ServerRunnerModule).port)
    else -> DirectPlayGameAbility(this)
}

private fun ExecutionContext.scenarioContexts() = listOfNotNull(
    ScenarioContext(ScenarioContext.ExecutionMode.DIRECT, ScenarioContext.Journal.IN_MEMORY),
    if (this.isHttpTest()) ScenarioContext(ScenarioContext.ExecutionMode.HTTP, ScenarioContext.Journal.IN_MEMORY) else null,
)

private fun ExecutionContext.isHttpTest(): Boolean =
    (isHttpTestForced() || isTagged("http")) && !isDirectTestForced()

private fun isHttpTestForced(): Boolean = "true" == System.getenv("FORCE_TEST_HTTP")

private fun isDirectTestForced(): Boolean = "true" == System.getenv("FORCE_TEST_DIRECT")
