package mastermind.game.acceptance.dsl.junit

import mastermind.game.acceptance.dsl.ScenarioContext
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolver

class ScenarioContextExtension : ParameterResolver {
    override fun supportsParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext) =
        parameterContext.parameter.type.isAssignableFrom(ScenarioContext::class.java)

    override fun resolveParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext) =
        ScenarioContext(
            if (extensionContext.isHttpTest()) ScenarioContext.ExecutionMode.HTTP
            else ScenarioContext.ExecutionMode.DIRECT
        )
}

private fun ExtensionContext.isHttpTest(): Boolean =
    (isHttpTestForced() || isTagged("http")) && !isDirectTestForced()

private fun ExtensionContext.isTagged(tagName: String) =
    tags.any { tag -> tag.equals(tagName, ignoreCase = true) }

private fun isHttpTestForced(): Boolean = "true" == System.getenv("FORCE_TEST_HTTP")

private fun isDirectTestForced(): Boolean = "true" == System.getenv("FORCE_TEST_DIRECT")
