package mastermind.game.testkit

import org.junit.jupiter.api.Assertions.assertEquals

infix fun <T> T?.shouldBe(expected: T?) {
    assertEquals(expected, this)
}
