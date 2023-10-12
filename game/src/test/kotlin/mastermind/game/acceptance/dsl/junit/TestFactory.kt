package mastermind.game.acceptance.dsl.junit

import org.junit.jupiter.api.DynamicContainer
import org.junit.jupiter.api.DynamicTest

fun dynamicContainer(
    displayName: String,
    tests: List<Pair<String, () -> Unit>>
): DynamicContainer = DynamicContainer.dynamicContainer(
    displayName,
    tests.map { (displayName, scenario) ->
        DynamicTest.dynamicTest(displayName, scenario)
    }
)
