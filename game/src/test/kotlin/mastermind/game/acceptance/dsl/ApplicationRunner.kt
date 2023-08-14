package mastermind.game.acceptance.dsl

interface ApplicationRunner : AutoCloseable {
    suspend fun start()

    suspend fun stop()

    fun playGameAbility(): PlayGameAbility
}