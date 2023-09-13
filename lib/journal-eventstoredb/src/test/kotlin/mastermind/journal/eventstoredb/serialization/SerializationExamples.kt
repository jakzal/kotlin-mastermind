package mastermind.journal.eventstoredb.serialization

import mastermind.journal.eventstoredb.serialization.SerializationExamples.TestEvent.Event1
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SerializationExamples {

    @Test
    fun `it writes an object to an array of bytes`() {
        val writer: (TestEvent) -> ByteArray = { _ -> """{"name":"First event"}""".toByteArray() }

        writer(Event1("First event")) shouldReturnJson """{"name":"First event"}"""
    }

    sealed interface TestEvent {
        data class Event1(val name: String) : TestEvent
    }
}

infix fun ByteArray.shouldReturnJson(expected: String) {
    assertEquals(expected, this.decodeToString(), "`$expected` is `${this.decodeToString()}`")
}
