package mastermind.game.testkit

import mastermind.game.MastermindApp
import mastermind.game.RunnerModule

class DirectRunnerModule : RunnerModule {
    context(MastermindApp)
    override fun start() {
    }

    context(MastermindApp)
    override fun shutdown() {
    }
}