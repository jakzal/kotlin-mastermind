package mastermind.journal.eventstoredb

import arrow.core.Either
import arrow.core.right
import com.fasterxml.jackson.core.io.JsonEOFException
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

private infix fun ByteArray.shouldReturnJson(expected: String) {
    assertEquals(expected, this.decodeToString(), "`$expected` is `${this.decodeToString()}`")
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
