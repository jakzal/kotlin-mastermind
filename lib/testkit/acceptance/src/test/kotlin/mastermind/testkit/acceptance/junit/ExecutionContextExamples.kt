package mastermind.testkit.acceptance.junit

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(ExecutionContextResolver::class)
class ExecutionContextExamples {
    @Test
    @Tag("foo")
    @Tag("bar")
    @Tag("baz")
    fun `it passes the execution context into the test`(context: ExecutionContext) {
        assertTrue(context.isTagged("foo"))
        assertTrue(context.isTagged("bar"))
        assertTrue(context.isTagged("baz"))
        assertFalse(context.isTagged("foobarbaz"))
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
