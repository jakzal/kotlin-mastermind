package mastermind.game.acceptance.dsl.http

import mastermind.game.MastermindApp
import mastermind.game.acceptance.dsl.ApplicationRunner
import mastermind.game.acceptance.dsl.PlayGameAbility
import mastermind.game.http.mastermindHttpApp
import org.http4k.server.Http4kServer
import org.http4k.server.Undertow
import org.http4k.server.asServer

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

    override fun close() {
        server.stop()
    }
}