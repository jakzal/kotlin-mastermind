package mastermind.game.acceptance.dsl.junit

import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolver

class ExecutionContextResolver : ParameterResolver {
    override fun supportsParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext) =
        parameterContext.parameter.type.isAssignableFrom(ExecutionContext::class.java)

    override fun resolveParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext) =
        ExecutionContext(extensionContext)
}