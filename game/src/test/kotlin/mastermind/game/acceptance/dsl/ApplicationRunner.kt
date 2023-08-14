package mastermind.game.acceptance.dsl

interface ApplicationRunner : AutoCloseable {
    suspend fun start()

    fun playGameAbility(): PlayGameAbility
}