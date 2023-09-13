package mastermind.journal.eventstoredb.serialization

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import mastermind.journal.eventstoredb.serialization.SerializationExamples.TestEvent.Event1
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SerializationExamples {

    @Test
    fun `it writes an object to an array of bytes`() {
        val writer: (TestEvent) -> ByteArray = createWriter(jacksonObjectMapper())

        writer(Event1("First event")) shouldReturnJson """{"name":"First event"}"""
    }

    sealed interface TestEvent {
        data class Event1(val name: String) : TestEvent
        data class Event2(val id: Int, val name: String) : TestEvent
    }
}

infix fun ByteArray.shouldReturnJson(expected: String) {
    assertEquals(expected, this.decodeToString(), "`$expected` is `${this.decodeToString()}`")
}

fun createWriter(objectMapper: ObjectMapper): (SerializationExamples.TestEvent) -> ByteArray =
    { event -> objectMapper.writeValueAsBytes(event) }
