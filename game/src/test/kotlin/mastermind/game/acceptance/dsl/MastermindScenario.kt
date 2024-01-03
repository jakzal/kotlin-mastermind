package mastermind.game.acceptance.dsl

import kotlinx.coroutines.test.runTest
import mastermind.eventstore.InMemoryEventStore
import mastermind.eventstore.eventstoredb.EventStoreDbEventStore
import mastermind.game.*
import mastermind.game.acceptance.dsl.direct.DirectPlayGameAbility
import mastermind.game.acceptance.dsl.http.HttpPlayGameAbility
import mastermind.game.http.ServerRunnerModule
import mastermind.game.testkit.DirectRunnerModule
import mastermind.testkit.acceptance.ExecutionContext
import mastermind.testkit.acceptance.isTagged
import org.junit.jupiter.api.DynamicContainer
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.DynamicTest.dynamicTest

class MastermindScenario(
    private val ability: PlayGameAbility,
    val secret: Code,
    val totalAttempts: Int,
    val availablePegs: Set<Code.Peg>
) : PlayGameAbility by ability

context(ExecutionContext)
fun mastermindScenario(
    secret: Code,
    totalAttempts: Int = 12,
    availablePegs: Set<Code.Peg> = setOfPegs("Red", "Green", "Blue", "Yellow", "Purple"),
    scenario: suspend MastermindScenario.() -> Unit
): List<DynamicTest> =
    scenarioContexts()
        .map { context ->
            with(context) {
                dynamicTest(mode.name, mastermindScenarioRunner(secret, totalAttempts, availablePegs, scenario))
            }
        }

context(ExecutionContext)
fun mastermindScenarios(
    scenarios: List<Pair<String, List<DynamicTest>>>
): List<DynamicContainer> =
    scenarios
        .map { (displayName, scenario) ->
            DynamicContainer.dynamicContainer(displayName, scenario)
        }

context(ScenarioExecutionContext)
private fun mastermindScenarioRunner(
    secret: Code,
    totalAttempts: Int,
    availablePegs: Set<Code.Peg>,
    scenario: suspend MastermindScenario.() -> Unit
): () -> Unit = {
    val app = MastermindApp(
        gameModule = GameModule(
            makeCode = { secret },
            totalAttempts = totalAttempts,
            availablePegs = availablePegs,
            eventStoreModule = eventStoreModule()
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

data class ScenarioExecutionContext(val mode: ExecutionMode, val eventStore: EventStore) {
    enum class ExecutionMode {
        HTTP,
        DIRECT
    }

    enum class EventStore {
        IN_MEMORY,
        EVENT_STORE_DB
    }
}

private fun ScenarioExecutionContext.runnerModule(): RunnerModule = when (mode) {
    ScenarioExecutionContext.ExecutionMode.HTTP -> ServerRunnerModule(0)
    ScenarioExecutionContext.ExecutionMode.DIRECT -> DirectRunnerModule()
}


private fun ScenarioExecutionContext.eventStoreModule(): EventStoreModule<GameEvent> = when (eventStore) {
    ScenarioExecutionContext.EventStore.IN_MEMORY -> EventStoreModule(InMemoryEventStore())
    ScenarioExecutionContext.EventStore.EVENT_STORE_DB -> EventStoreModule(EventStoreDbEventStore("esdb://localhost:2113?tls=false"))
}

private fun MastermindApp.playGameAbility(): PlayGameAbility = when (runnerModule) {
    is ServerRunnerModule -> HttpPlayGameAbility((runnerModule as ServerRunnerModule).port)
    else -> DirectPlayGameAbility(this)
}

private fun ExecutionContext.scenarioContexts() = listOfNotNull(
    ScenarioExecutionContext(ScenarioExecutionContext.ExecutionMode.DIRECT, ScenarioExecutionContext.EventStore.IN_MEMORY),
    if (this.isHttpTest()) ScenarioExecutionContext(
        ScenarioExecutionContext.ExecutionMode.HTTP,
        ScenarioExecutionContext.EventStore.IN_MEMORY
    ) else null,
)

private fun ExecutionContext.isHttpTest(): Boolean =
    (isHttpTestForced() || isTagged("http")) && !isDirectTestForced()

private fun isHttpTestForced(): Boolean = "true" == System.getenv("FORCE_TEST_HTTP")

private fun isDirectTestForced(): Boolean = "true" == System.getenv("FORCE_TEST_DIRECT")
