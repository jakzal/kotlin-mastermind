package mastermind.journal.eventstoredb

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlin.reflect.KClass

fun <T : Any> createWriter(objectMapper: ObjectMapper = jacksonObjectMapper()): T.() -> ByteArray =
    objectMapper::writeValueAsBytes

fun <T : Any> createReader(objectMapper: ObjectMapper = jacksonObjectMapper()): ByteArray.(KClass<out T>) -> T =
    { type -> objectMapper.readValue(this, type.java) }
