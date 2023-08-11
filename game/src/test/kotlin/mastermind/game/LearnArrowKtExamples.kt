package mastermind.game

import arrow.core.NonEmptyList
import arrow.core.nonEmptyListOf
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

@Tag("learning")
class LearnArrowKtExamples {
    @Test
    fun `it requires at least one element to be present`() {
        val list = nonEmptyListOf("Item 1", "Item 2", "Item 3")

        assertEquals(listOf("Item 1", "Item 2", "Item 3"), list)
        assertEquals(NonEmptyList("Item 1", listOf("Item 2", "Item 3")), list)
        assertEquals("Item 1", list.head)
        assertEquals(listOf("Item 2", "Item 3"), list.tail)
        assertEquals("Item 1,Item 2,Item 3", list.reduce { acc, i -> "$acc,$i" })
    }
}