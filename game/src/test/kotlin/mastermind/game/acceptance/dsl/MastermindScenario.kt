package mastermind.game.acceptance.dsl

import kotlinx.coroutines.runBlocking
import mastermind.game.Code
import mastermind.game.Configuration
import mastermind.game.MastermindApp
import mastermind.game.acceptance.dsl.direct.DirectApplicationRunner
import mastermind.game.acceptance.dsl.http.HttpApplicationRunner
import mastermind.game.setOfPegs

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
                configuration = Configuration(
                    makeCode = { secret }
                )
            )
            runBlocking {
                val runner = applicationRunnerFor(app)
                runner.start()
                runner.use {
                    MastermindScenario(runner.playGameAbility(), secret, totalAttempts, availablePegs)
                        .scenario()
                }
            }
        }
    }
}

data class ScenarioContext(val mode: ExecutionMode) {
    enum class ExecutionMode {
        HTTP,
        DIRECT
    }

}

private fun ScenarioContext.applicationRunnerFor(app: MastermindApp): ApplicationRunner = when (mode) {
    ScenarioContext.ExecutionMode.HTTP -> HttpApplicationRunner(app)
    ScenarioContext.ExecutionMode.DIRECT -> DirectApplicationRunner(app)
}
