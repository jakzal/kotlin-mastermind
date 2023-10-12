package mastermind.game.acceptance.dsl.junit

import org.junit.jupiter.api.TestFactory

@TestFactory
@Target(AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Scenario
