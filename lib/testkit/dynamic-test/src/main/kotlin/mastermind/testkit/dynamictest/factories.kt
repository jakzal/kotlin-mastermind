package mastermind.testkit.dynamictest

import org.junit.jupiter.api.DynamicTest

fun <T : Any> Map<String, T>.dynamicTestsFor(block: (T) -> Unit) =
    map { (message, example: T) -> DynamicTest.dynamicTest(message) { block(example) } }
