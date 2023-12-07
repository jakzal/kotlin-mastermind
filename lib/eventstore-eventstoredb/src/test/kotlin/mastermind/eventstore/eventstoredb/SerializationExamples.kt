package mastermind.eventstore.eventstoredb

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.PropertyAccessor
import com.fasterxml.jackson.core.io.JsonEOFException
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonMapperBuilder
import mastermind.eventstore.eventstoredb.SerializationExamples.TestEvent.Event1
import mastermind.eventstore.eventstoredb.SerializationExamples.TestEvent.Event2
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class SerializationExamples {

    @Test
    fun `it writes an object to an array of bytes`() {
        val asJson = createWriter<TestEvent>()

        Event1("First event").asJson() shouldReturnJson """{"name":"First event"}"""
    }

    @Test
    fun `it throws an exception if it fails to write the value`() {
        val mapper = jacksonMapperBuilder()
            .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, true)
            .build()
        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.PUBLIC_ONLY)
        val asJson = createWriter<Any>(mapper)

        assertThrows<JsonMappingException> {
            object {
                @Suppress("unused")
                val name: String get() = throw RuntimeException("Writing failed.")
            }.asJson()
        }
    }

    @Test
    fun `it reads an array of bytes to an object`() {
        val asObject = createReader<TestEvent>()
        val bytes = """{"id":13, "name":"Second event"}""".toByteArray()

        assertEquals(Event2(13, "Second event"), bytes.asObject(Event2::class))
    }

    @Test
    fun `it throws an exception if the object cannot be read`() {
        val asObject = createReader<TestEvent>()
        val malformedBytes = """{"id":13, "name":"Second event"""".toByteArray()

        assertThrows<JsonEOFException> {
            malformedBytes.asObject(Event2::class)
        }
    }

    sealed interface TestEvent {
        data class Event1(val name: String) : TestEvent
        data class Event2(val id: Int, val name: String) : TestEvent
    }
}

private infix fun ByteArray.shouldReturnJson(expected: String) {
    assertEquals(expected, this.decodeToString(), "`$expected` is `${this.decodeToString()}`")
}
