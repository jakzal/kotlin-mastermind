package mastermind.game.acceptance.dsl.http

import mastermind.game.MastermindApp
import mastermind.game.acceptance.dsl.ApplicationRunner
import mastermind.game.acceptance.dsl.PlayGameAbility
import mastermind.game.http.routes
import mastermind.game.http.serverFor
import org.http4k.server.Http4kServer

class HttpApplicationRunner(app: MastermindApp) : ApplicationRunner {
    private val server: Http4kServer = serverFor(0, app.routes)

    override suspend fun start() {
        server.start()
    }

    override fun playGameAbility(): PlayGameAbility {
        return HttpPlayGameAbility(server.port())
    }

    override fun close() {
        server.stop()
    }
}