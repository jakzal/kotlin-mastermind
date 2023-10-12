package mastermind.game.acceptance.dsl.junit

import mastermind.game.acceptance.dsl.ExecutionPlan
import mastermind.game.acceptance.dsl.ScenarioContext
import mastermind.game.acceptance.dsl.ScenarioContext.ExecutionMode
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolver

class ExecutionPlanResolver : ParameterResolver {
    override fun supportsParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext) =
        parameterContext.parameter.type.isAssignableFrom(ExecutionPlan::class.java)

    override fun resolveParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext) =
        ExecutionPlan(
            listOfNotNull(
                ScenarioContext(ExecutionMode.DIRECT),
                if (extensionContext.isHttpTest()) ScenarioContext(ExecutionMode.HTTP) else null,
            )
        )
}

private fun ExtensionContext.isHttpTest(): Boolean =
    (isHttpTestForced() || isTagged("http")) && !isDirectTestForced()

private fun ExtensionContext.isTagged(tagName: String) =
    tags.any { tag -> tag.equals(tagName, ignoreCase = true) }

private fun isHttpTestForced(): Boolean = "true" == System.getenv("FORCE_TEST_HTTP")

private fun isDirectTestForced(): Boolean = "true" == System.getenv("FORCE_TEST_DIRECT")
