package mastermind.testkit.acceptance.junit

import org.junit.jupiter.api.extension.ExtensionContext

data class ExecutionContext(private val extensionContext: ExtensionContext) {
    private val tags: Set<String> get() = extensionContext.tags

    fun isTagged(tagName: String) = tags.any { tag ->
        tag.equals(tagName, ignoreCase = true)
    }
}
