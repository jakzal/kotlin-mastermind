package mastermind.command

import arrow.core.NonEmptyList
import arrow.core.left
import arrow.core.nonEmptyListOf
import arrow.core.right
import mastermind.command.fixtures.TestCommand
import mastermind.command.fixtures.TestError
import mastermind.command.fixtures.TestEvent
import mastermind.eventsourcing.Execute
import mastermind.eventsourcing.handlerFor
import mastermind.testkit.assertions.shouldBe
import mastermind.testkit.assertions.shouldFailWith
import mastermind.testkit.assertions.shouldSucceedWith
import org.junit.jupiter.api.Test

class NoStateHandlerExamples {
    @Test
    fun `it executes the command with no state if there is no events`() {
        val expectedEvent = TestEvent("Event 1")
        val invokedCommand = TestCommand("Command 1")
        val execute: Execute<TestCommand, NonEmptyList<TestEvent>?, TestError, TestEvent> =
            { command, events ->
                nonEmptyListOf(expectedEvent).right().also {
                    command shouldBe invokedCommand
                    events shouldBe null
                }
            }

        val handler = handlerFor(execute)

        handler(invokedCommand, emptyList()) shouldSucceedWith nonEmptyListOf(expectedEvent)
    }

    @Test
    fun `it executes the command with the list of events if there are any`() {
        val expectedEvent = TestEvent("Event 4")
        val invokedCommand = TestCommand("Command 2")
        val eventHistory = listOf(TestEvent("Event 1"), TestEvent("Event 2"), TestEvent("Event 3"))
        val execute: Execute<TestCommand, NonEmptyList<TestEvent>?, TestError, TestEvent> =
            { command, events ->
                nonEmptyListOf(expectedEvent).right().also {
                    command shouldBe invokedCommand
                    events shouldBe eventHistory
                }
            }

        val handler = handlerFor(execute)

        handler(invokedCommand, eventHistory) shouldSucceedWith nonEmptyListOf(expectedEvent)
    }

    @Test
    fun `it returns the error if command execution fails`() {
        val expectedError = TestError("Error 1")
        val execute: Execute<TestCommand, NonEmptyList<TestEvent>?, TestError, TestEvent> =
            { _, _ -> expectedError.left() }

        val handler = handlerFor(execute)

        handler(TestCommand("Command 1"), emptyList()) shouldFailWith expectedError
    }
}