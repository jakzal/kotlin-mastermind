package mastermind.journal.eventstoredb

import arrow.core.Either
import arrow.core.right
import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.PropertyAccessor
import com.fasterxml.jackson.core.io.JsonEOFException
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonMapperBuilder
import mastermind.journal.eventstoredb.SerializationExamples.TestEvent.Event1
import mastermind.journal.eventstoredb.SerializationExamples.TestEvent.Event2
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test

class SerializationExamples {

    @Test
    fun `it writes an object to an array of bytes`() {
        val asJson = createWriter<TestEvent>()

        Event1("First event").asJson() shouldReturnJson """{"name":"First event"}"""
    }

    @Test
    fun `it returns an error if it fails to write the value`() {
        val mapper = jacksonMapperBuilder()
            .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, true)
            .build()
        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.PUBLIC_ONLY)
        val asJson = createWriter<Any>(mapper)

        object {
            @Suppress("unused")
            val name: String get() = throw RuntimeException("Writing failed.")
        }.asJson() shouldFailWith { failure: WriteFailure ->
            failure.cause shouldHaveTypeOf JsonMappingException::class.java
        }
    }

    @Test
    fun `it reads an array of bytes to an object`() {
        val asObject = createReader<TestEvent>()
        val bytes = """{"id":13, "name":"Second event"}""".toByteArray()

        bytes.asObject(Event2::class) shouldSucceedWith Event2(13, "Second event")
    }

    @Test
    fun `it returns an error if the object cannot be read`() {
        val asObject = createReader<TestEvent>()
        val malformedBytes = """{"id":13, "name":"Second event"""".toByteArray()

        malformedBytes.asObject(Event2::class) shouldFailWith { failure: ReadFailure ->
            failure.cause shouldHaveTypeOf JsonEOFException::class.java
        }
    }

    sealed interface TestEvent {
        data class Event1(val name: String) : TestEvent
        data class Event2(val id: Int, val name: String) : TestEvent
    }
}

private infix fun Either<WriteFailure, ByteArray>.shouldReturnJson(expected: String) {
    this.onLeft { fail("Expected a success but g ot: `$this`.") }
        .map { bytes ->
            assertEquals(expected, bytes.decodeToString(), "`$expected` is `${bytes.decodeToString()}`")
        }
}

private infix fun <T> T.shouldSucceedWith(expected: T) {
    assertEquals(expected.right(), this, "`$expected` is `$this`")
}

private infix fun <A, B> Either<A, B>.shouldFailWith(onFailure: (A) -> Unit) {
    this
        .onRight { fail("Expected a failure but got: `$this`.") }
        .mapLeft(onFailure)
}

private infix fun Any.shouldHaveTypeOf(type: Class<out Throwable>) {
    assertEquals(type, this::class.java)
}
