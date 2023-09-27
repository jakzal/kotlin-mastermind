package mastermind.journal.eventstoredb

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlin.reflect.KClass

data class WriteError(val cause: Throwable)
data class ReadError(val cause: Throwable)

fun <T : Any> createWriter(objectMapper: ObjectMapper = jacksonObjectMapper()): T.() -> Either<WriteError, ByteArray> =
    {
        try {
            objectMapper.writeValueAsBytes(this).right()
        } catch (cause: Throwable) {
            WriteError(cause).left()
        }
    }

fun <T : Any> createReader(objectMapper: ObjectMapper = jacksonObjectMapper()): ByteArray.(KClass<out T>) -> Either<ReadError, T> =
    { type ->
        try {
            objectMapper.readValue(this, type.java).right()
        } catch (cause: Throwable) {
            ReadError(cause).left()
        }
    }
