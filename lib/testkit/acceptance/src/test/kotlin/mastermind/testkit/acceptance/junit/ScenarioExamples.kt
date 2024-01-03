package mastermind.testkit.acceptance.junit

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Tag

class ScenarioExamples {
    @Scenario
    fun `it registers a test factory`() = listOf(
        dynamicTest("Case 1") { assertTrue(true) },
        dynamicTest("Case 2") { assertFalse(false) }
    )

    @Scenario
    @Tag("foo")
    fun `it passes the execution context into the test`(executionContext: ExecutionContext) = listOf(
        dynamicTest("Case 1") { assertTrue(executionContext.isTagged("foo")) },
        dynamicTest("Case 2") { assertFalse(executionContext.isTagged("bar")) }
    )
}
