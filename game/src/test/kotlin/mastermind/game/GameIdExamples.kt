package mastermind.game

import mastermind.testkit.assertions.shouldBe
import mastermind.testkit.assertions.shouldMatch
import mastermind.testkit.assertions.shouldNotBe
import org.junit.jupiter.api.Test

class GameIdExamples {
    @Test
    fun `it is created from string value`() {
        val gameId = GameId("838dd490-54a5-412c-ae5b-563311d1394e")

        gameId.value shouldBe "838dd490-54a5-412c-ae5b-563311d1394e"
    }

    @Test
    fun `it generates a new game id`() {
        val gameId = generateGameId()

        gameId.value shouldMatch "[0-9a-z]{8}-[0-9a-z]{4}-[0-9a-z]{4}-[0-9a-z]{4}-[0-9a-z]{12}"
    }

    @Test
    fun `it generates a new value each time`() {
        generateGameId() shouldNotBe generateGameId()
    }
}
