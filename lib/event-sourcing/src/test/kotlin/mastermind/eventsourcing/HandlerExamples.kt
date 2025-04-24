package mastermind.eventsourcing

import arrow.core.left
import arrow.core.nonEmptyListOf
import arrow.core.right
import mastermind.eventsourcing.fixtures.TestCommand
import mastermind.eventsourcing.fixtures.TestError
import mastermind.eventsourcing.fixtures.TestEvent
import mastermind.eventsourcing.fixtures.TestState
import mastermind.testkit.assertions.shouldBe
import mastermind.testkit.assertions.shouldFailWith
import mastermind.testkit.assertions.shouldSucceedWith
import org.junit.jupiter.api.Test

class HandlerExamples {
    @Test
    fun `it reconstructs a nullable state from history of events`() {
        val expectedEvent = TestEvent("Event 1")
        val eventHistory = listOf(TestEvent("Event 1"), TestEvent("Event 2"))
        val invokedCommand = TestCommand("Command 1")

        val applyEvent: Apply<TestState?, TestEvent> = { state, event ->
            state?.let { TestState(it.history + event.id) } ?: TestState(listOf(event.id))
        }
        val execute: Execute<TestCommand, TestState?, TestError, TestEvent> =
            { command, state ->
                nonEmptyListOf(expectedEvent).right().also {
                    command shouldBe invokedCommand
                    state shouldBe TestState(listOf("Event 1", "Event 2"))
                }
            }

        val handler = handlerFor(execute, applyEvent) { null }

        handler(invokedCommand, eventHistory) shouldSucceedWith nonEmptyListOf(expectedEvent)
    }

    @Test
    fun `it reconstructs a non-nullable state from history of events`() {
        val expectedEvent = TestEvent("Event 1")
        val eventHistory = listOf(TestEvent("Event 1"), TestEvent("Event 2"))
        val invokedCommand = TestCommand("Command 1")

        val applyEvent: Apply<TestState, TestEvent> = { state, event ->
            TestState(state.history + event.id)
        }
        val execute: Execute<TestCommand, TestState, TestError, TestEvent> =
            { command, state ->
                nonEmptyListOf(expectedEvent).right().also {
                    command shouldBe invokedCommand
                    state shouldBe TestState(listOf("Event 1", "Event 2"))
                }
            }

        val handler = handlerFor(execute, applyEvent) { TestState(emptyList()) }

        handler(invokedCommand, eventHistory) shouldSucceedWith nonEmptyListOf(expectedEvent)
    }

    @Test
    fun `it returns the error if command execution fails`() {
        val expectedError = TestError("Error 1")

        val applyEvent: Apply<TestState, TestEvent> = { state, event -> TestState(state.history + event.id) }
        val execute: Execute<TestCommand, TestState, TestError, TestEvent> = { _, _ -> expectedError.left() }

        val handler = handlerFor(execute, applyEvent) { TestState(emptyList()) }

        handler(TestCommand("Command 1"), emptyList()) shouldFailWith expectedError
    }
}
