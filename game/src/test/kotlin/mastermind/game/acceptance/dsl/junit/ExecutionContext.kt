package mastermind.game.acceptance.dsl.junit

import org.junit.jupiter.api.extension.ExtensionContext

data class ExecutionContext(private val extensionContext: ExtensionContext) {
    fun isTagged(tagName: String) =
        extensionContext.tags.any { tag -> tag.equals(tagName, ignoreCase = true) }
}