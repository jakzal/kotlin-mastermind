package mastermind.game.acceptance.dsl

import kotlinx.coroutines.runBlocking
import mastermind.game.Code
import mastermind.game.Configuration
import mastermind.game.MastermindApp
import mastermind.game.acceptance.dsl.http.HttpPlayGameAbility
import mastermind.game.http.mastermindHttpApp
import org.http4k.server.Http4kServer
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

            val app = MastermindApp(
                configuration = Configuration(
                    codeMaker = { secret }
                )
            )
            runBlocking {
                val runner = HttpApplicationRunner(app)
                try {
                    runner.start()
                    MastermindScenario(runner.playGameAbility(), secret, totalAttempts)
                        .scenario()
                } finally {
                    runner.stop()
                }
            }
        }
    }
}

interface ApplicationRunner {
    suspend fun start()

    suspend fun stop()
    fun playGameAbility(): PlayGameAbility
}

class HttpApplicationRunner(app: MastermindApp) : ApplicationRunner {
    private val server: Http4kServer = mastermindHttpApp(app).asServer(Undertow(0))

    override suspend fun start() {
        server.start()
    }

    override suspend fun stop() {
        server.stop()
    }

    override fun playGameAbility(): PlayGameAbility {
        return HttpPlayGameAbility(server.port())
    }
}
