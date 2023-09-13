package mastermind.journal.eventstoredb

import mastermind.journal.eventstoredb.SerializationExamples.TestEvent.Event1
import mastermind.journal.eventstoredb.SerializationExamples.TestEvent.Event2
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SerializationExamples {

    @Test
    fun `it writes an object to an array of bytes`() {
        val asJson = createWriter<TestEvent>()

        Event1("First event").asJson() shouldReturnJson """{"name":"First event"}"""
    }

    @Test
    fun `it reads an array of bytes to an object`() {
        val asObject = createReader<Event2>()
        val bytes = """{"id":13, "name":"Second event"}""".toByteArray()

        bytes.asObject(Event2::class.java) shouldReturn Event2(13, "Second event")
    }

    sealed interface TestEvent {
        data class Event1(val name: String) : TestEvent
        data class Event2(val id: Int, val name: String) : TestEvent
    }
}

infix fun ByteArray.shouldReturnJson(expected: String) {
    assertEquals(expected, this.decodeToString(), "`$expected` is `${this.decodeToString()}`")
}

infix fun <T> T.shouldReturn(expected: T) {
    assertEquals(expected, this, "`$expected` is `$this`")
}
