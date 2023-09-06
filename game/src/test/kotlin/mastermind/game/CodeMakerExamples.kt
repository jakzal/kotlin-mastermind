package mastermind.game

import mastermind.game.testkit.shouldBe
import mastermind.game.testkit.shouldNotBe
import org.junit.jupiter.api.Test

class CodeMakerExamples {
    @Test
    fun `it makes a code of the given length`() {
        val code = makeCode(length = 5)

        code.length shouldBe 5
    }

    @Test
    fun `it makes a random code`() {
        // It's possible to get the same code in two subsequent calls,
        // but we're going to go for it anyway since it's rather unlikely with longer codes.
        makeCode(length = 10) shouldNotBe makeCode(length = 10)
    }

    @Test
    fun `it makes a code of length 4 by default`() {
        makeCode().length shouldBe 4
    }
}