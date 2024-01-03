package mastermind.testkit.acceptance.junit

import mastermind.testkit.acceptance.ExecutionContext
import mastermind.testkit.acceptance.isTagged
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(JUnitExecutionContextResolver::class)
class JUnitExecutionContextExamples {
    @Test
    @Tag("foo")
    @Tag("bar")
    @Tag("baz")
    fun `it passes the execution context into the test`(context: ExecutionContext) {
        assertEquals(setOf("foo", "bar", "baz"), context.tags())
    }

    @Test
    @Tag("foo")
    @Tag("Bar")
    @Tag("BAZ")
    fun `it ignores case when checking if tag is present`(context: ExecutionContext) {
        assertTrue(context.isTagged("foo"))
        assertTrue(context.isTagged("bar"))
        assertTrue(context.isTagged("baz"))
        assertFalse(context.isTagged("foobarbaz"))
    }
}
