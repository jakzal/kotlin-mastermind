package mastermind.game.acceptance.dsl.direct

import mastermind.game.MastermindApp
import mastermind.game.acceptance.dsl.ApplicationRunner
import mastermind.game.acceptance.dsl.PlayGameAbility

class DirectApplicationRunner(private val app: MastermindApp) : ApplicationRunner {
    override suspend fun start() {
    }

    override fun playGameAbility(): PlayGameAbility = DirectPlayGameAbility(app)

    override fun close() {
    }
}