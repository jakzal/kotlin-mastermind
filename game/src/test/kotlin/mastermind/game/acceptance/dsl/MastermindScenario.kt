package mastermind.game.acceptance.dsl

import kotlinx.coroutines.runBlocking
import mastermind.game.Code
import mastermind.game.Configuration
import mastermind.game.MastermindApp
import mastermind.game.acceptance.dsl.http.HttpApplicationRunner

class MastermindScenario(
    private val ability: PlayGameAbility,
    val secret: Code,
    val totalAttempts: Int
) : PlayGameAbility by ability {
    companion object {
        operator fun invoke(
            secret: Code,
            totalAttempts: Int = 12,
            scenario: suspend MastermindScenario.() -> Unit
        ) {

            val app = MastermindApp(
                configuration = Configuration(
                    codeMaker = { secret }
                )
            )
            runBlocking {
                val runner = HttpApplicationRunner(app)
                runner.start()
                runner.use {
                    MastermindScenario(runner.playGameAbility(), secret, totalAttempts)
                        .scenario()
                }
            }
        }
    }
}
