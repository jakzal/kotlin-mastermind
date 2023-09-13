package mastermind.journal.eventstoredb

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlin.reflect.KClass

data class ReadFailure(val cause: Throwable)

fun <T : Any> createWriter(objectMapper: ObjectMapper = jacksonObjectMapper()): T.() -> ByteArray =
    objectMapper::writeValueAsBytes

fun <T : Any> createReader(objectMapper: ObjectMapper = jacksonObjectMapper()): ByteArray.(KClass<out T>) -> Either<ReadFailure, T> =
    { type ->
        try {
            objectMapper.readValue(this, type.java).right()
        } catch (cause: Throwable) {
            ReadFailure(cause).left()
        }
    }
