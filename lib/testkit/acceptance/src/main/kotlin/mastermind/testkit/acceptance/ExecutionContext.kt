package mastermind.testkit.acceptance

interface ExecutionContext {
    fun tags(): Set<String>
}

fun ExecutionContext.isTagged(tagName: String) = tags().any { tag ->
    tag.equals(tagName, ignoreCase = true)
}
