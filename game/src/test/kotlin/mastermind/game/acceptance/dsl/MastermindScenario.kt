package mastermind.game.acceptance.dsl

import kotlinx.coroutines.runBlocking
import mastermind.game.Code
import mastermind.game.Configuration
import mastermind.game.MastermindApp
import mastermind.game.acceptance.dsl.http.HttpPlayGameAbility
import mastermind.game.http.mastermindHttpApp
import org.http4k.server.Undertow
import org.http4k.server.asServer

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
            val server = mastermindHttpApp(MastermindApp(
                configuration = Configuration(
                    codeMaker = { secret }
                )
            )).asServer(Undertow(0)).start()
            runBlocking {
                MastermindScenario(HttpPlayGameAbility(server.port()), secret, totalAttempts).scenario()
            }
            server.stop()
        }
    }
}

