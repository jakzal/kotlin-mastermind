package mastermind.game.acceptance.dsl.junit

import mastermind.testkit.acceptance.junit.JUnitExecutionContextResolver
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.extension.ExtendWith

@TestFactory
@Target(AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@ExtendWith(JUnitExecutionContextResolver::class)
annotation class Scenario
