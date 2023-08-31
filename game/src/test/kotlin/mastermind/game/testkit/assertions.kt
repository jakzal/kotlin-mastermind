package mastermind.game.testkit

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals

infix fun <T> T?.shouldBe(expected: T?) {
    assertEquals(expected, this)
}

infix fun <T> T?.shouldReturn(expected: T?) = shouldBe(expected)

infix fun <T> T?.shouldNotBe(expected: T?) {
    assertNotEquals(expected, this)
}

infix fun String.shouldMatch(pattern: String) {
    Assertions.assertTrue(pattern.toRegex().matches(this), "`$this` matches `$pattern`")
}

infix fun <A, B> Either<A, B>.shouldSucceedWith(expected: B) {
    this shouldReturn expected.right()
}

infix fun <A, B> Either<A, B>.shouldFailWith(expected: A) {
    this shouldReturn expected.left()
}
