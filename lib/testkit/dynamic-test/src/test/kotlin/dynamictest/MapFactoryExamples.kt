package dynamictest

import mastermind.testkit.dynamictest.dynamicTestsFor
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.TestFactory

class MapFactoryExamples {

    @TestFactory
    fun `it executes examples found in a map`() = examples { (lowercaseLetter, uppercaseLetter) ->
        assertEquals(lowercaseLetter, uppercaseLetter.lowercase())
    }

    private fun examples(block: (Pair<String, String>) -> Unit) = mapOf(
        "First example" to Pair("a", "A"),
        "Second example" to Pair("b", "B"),
        "Third example" to Pair("c", "C"),
    ).dynamicTestsFor(block)
}
