package mastermind.testkit

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import org.junit.jupiter.api.Assertions.*

infix fun <T> T?.shouldBe(expected: T?) {
    assertEquals(expected, this, "$expected is $this")
}

infix fun <T> T?.shouldNotBe(expected: T?) {
    assertNotEquals(expected, this, "$expected is not $this")
}

infix fun String.shouldMatch(pattern: String) {
    assertTrue(pattern.toRegex().matches(this), "`$this` matches `$pattern`")
}

infix fun <T> T?.shouldReturn(expected: T?) = shouldBe(expected)

infix fun <T> T?.shouldNotReturn(expected: T?) = shouldNotBe(expected)

infix fun <A, B> Either<A, B>.shouldSucceedWith(expected: B) = this shouldBeSuccessOf expected

infix fun <A, B> Either<A, B>.shouldFailWith(expected: A) = this shouldBeFailureOf expected

infix fun <A, B> Either<A, B>.shouldBeSuccessOf(expected: B) = this shouldReturn expected.right()

infix fun <A, B> Either<A, B>.shouldBeFailureOf(expected: A) = this shouldReturn expected.left()
