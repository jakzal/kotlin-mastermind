package mastermind.testkit.acceptance.junit

import mastermind.testkit.acceptance.ExecutionContext
import org.junit.jupiter.api.extension.ExtensionContext

data class JUnitExecutionContext(private val extensionContext: ExtensionContext) : ExecutionContext {
    override fun tags(): Set<String> = extensionContext.tags
}
