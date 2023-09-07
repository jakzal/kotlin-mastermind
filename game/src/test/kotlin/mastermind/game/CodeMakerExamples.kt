package mastermind.game

import mastermind.testkit.assertions.shouldBe
import mastermind.testkit.assertions.shouldNotBe
import org.junit.jupiter.api.Test

class CodeMakerExamples {
    @Test
    fun `it makes a code of the given length`() {
        val code = setOfPegs("Red", "Green", "Blue")
            .makeCode(length = 5)

        code.length shouldBe 5
    }

    @Test
    fun `it makes a random code`() {
        val pegs = setOfPegs("Red", "Green", "Blue", "Yellow", "Purple")
        // It's possible to get the same code in two subsequent calls,
        // but we're going to go for it anyway since it's rather unlikely with longer codes.
        pegs.makeCode(length = 10) shouldNotBe pegs.makeCode(length = 10)
    }

    @Test
    fun `it makes a code of length 4 by default`() {
        setOfPegs("Red", "Green", "Blue").makeCode().length shouldBe 4
    }

    @Test
    fun `it makes a code out of the given pegs only`() {
        val code = setOfPegs("Red").makeCode(length = 5)

        code shouldBe Code("Red", "Red", "Red", "Red", "Red")
    }
}