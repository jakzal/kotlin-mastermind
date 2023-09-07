package mastermind.testkit.assertions

import arrow.core.left
import arrow.core.right
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.opentest4j.AssertionFailedError

class ShouldExamples {
    @Test
    fun `shouldBe passes if two values are the same`() {
        "A" shouldBe "A"
    }

    @Test
    fun `shouldBe fails if two values are different`() {
        assertThrows<AssertionFailedError> {
            "A" shouldBe "B"
        }
    }

    @Test
    fun `shouldNotBe passes if two values are different`() {
        "A" shouldNotBe "B"
    }

    @Test
    fun `shouldNotBe fails if two values are the same`() {
        assertThrows<AssertionFailedError> {
            "A" shouldNotBe "A"
        }
    }

    @Test
    fun `shouldReturn passes if two values are the same`() {
        getMeA() shouldReturn "A"
    }

    @Test
    fun `shouldReturn fails if two values are different`() {
        assertThrows<AssertionFailedError> {
            getMeA() shouldReturn "B"
        }
    }

    @Test
    fun `shouldNotReturn passes if two values are the same`() {
        getMeA() shouldNotReturn "B"
    }

    @Test
    fun `shouldNotReturn fails if two values are different`() {
        assertThrows<AssertionFailedError> {
            getMeA() shouldNotReturn "A"
        }
    }

    @Test
    fun `shouldMatch passes if the regular expression evaluates to true`() {
        "aaa" shouldMatch "a{3}"
    }

    @Test
    fun `shouldMatch fails if the regular expression evaluates to false`() {
        assertThrows<AssertionFailedError> {
            "aaa" shouldMatch "a{5}"
        }
    }

    @Test
    fun `shouldSucceedWith passes if the result is the right value of either`() {
        getRightA() shouldSucceedWith "A"
    }

    @Test
    fun `shouldSucceedWith fails if the result is the left value of either`() {
        assertThrows<AssertionFailedError> {
            getLeftA() shouldSucceedWith "A"
        }
    }

    @Test
    fun `shouldFailWith passes if the result is the left value of either`() {
        getLeftA() shouldFailWith "A"
    }

    @Test
    fun `shouldFailWith fails if the result is the right value of either`() {
        assertThrows<AssertionFailedError> {
            getRightA() shouldFailWith "A"
        }
    }

    @Test
    fun `shouldBeSuccessOf passes if the result is the right value of either`() {
        "A".right() shouldBeSuccessOf "A"
    }

    @Test
    fun `shouldBeSuccessOf fails if the result is the left value of either`() {
        assertThrows<AssertionFailedError> {
            "A".left() shouldBeSuccessOf "A"
        }
    }

    @Test
    fun `shouldBeFailureOf passes if the result is the left value of either`() {
        "A".left() shouldBeFailureOf "A"
    }

    @Test
    fun `shouldBeFailureOf fails if the result is the right value of either`() {
        assertThrows<AssertionFailedError> {
            "A".right() shouldBeFailureOf "A"
        }
    }

    private fun getMeA() = "A"
    private fun getRightA() = "A".right()
    private fun getLeftA() = "A".left()
}
