package mastermind.game

import arrow.core.*
import arrow.core.raise.Raise
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.zipOrAccumulate
import org.junit.jupiter.api.Assertions
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

    @Test
    fun `it converts a value to an error case`() {
        fun findUser(id: UserId): Either<UserNotFound, User> {
            return UserNotFound("User not found for ID ${id.value}").left()
        }

        val result = findUser(UserId("ABC-123"))

        assertEquals(UserNotFound("User not found for ID ABC-123"), result.leftOrNull())
    }

    @Test
    fun `it converts a value to a success case`() {
        fun findUser(id: UserId): Either<UserNotFound, User> {
            return User(id).right()
        }

        val result = findUser(UserId("ABC-123"))

        assertEquals(User(UserId("ABC-123")), result.getOrNull())
    }

    @Test
    fun `it raises a value to an error case`() {
        fun Raise<UserNotFound>.findUser(id: UserId): User {
            raise(UserNotFound("User not found for ID ${id.value}"))
        }

        val result = either {
            findUser(UserId("ABC-123"))
        }

        assertEquals(UserNotFound("User not found for ID ABC-123"), result.leftOrNull())
    }

    @Test
    fun `it raises a value to a success case`() {
        @Suppress("UnusedReceiverParameter")
        fun Raise<UserNotFound>.findUser(id: UserId): User {
            return User(id)
        }

        val result = either {
            findUser(UserId("ABC-123"))
        }

        assertEquals(User(UserId("ABC-123")), result.getOrNull())
    }

    @Test
    fun `it ensures a value meets criteria`() {
        fun findUser(id: UserId): Either<UserNotFound, User> = either {
            ensure(id.value.startsWith("ABC-")) { UserNotFound("User not found for ID ${id.value}") }
            return User(id).right()
        }

        assertEquals(UserNotFound("User not found for ID XYZ-123").left(), findUser(UserId("XYZ-123")))
        assertEquals(User(UserId("ABC-123")).right(), findUser(UserId("ABC-123")))
    }

    @Test
    fun `it inspects results`() {
        fun findUser(id: UserId): Either<UserNotFound, User> = either {
            ensure(id.value.startsWith("ABC-")) { UserNotFound("User not found for ID ${id.value}") }
            return User(id).right()
        }

        when (val result = findUser(UserId("ABC-123"))) {
            is Either.Left -> Assertions.fail("Expected a success")
            is Either.Right -> assertEquals(UserId("ABC-123"), result.value.userId)
        }
    }

    @Test
    fun `it uses the raise DSL to bind sub-computations`() {
        val maybeTwo: Either<Problem, Int> = either { 2 }
        val maybeFive: Either<Problem, Int> = either { raise(Problem) }

        val maybeSeven: Either<Problem, Int> = either {
            maybeTwo.bind() + maybeFive.bind()
        }

        assertEquals(Problem.left(), maybeSeven)
    }

    @Test
    fun `it accumulates errors`() {
        fun findUser(id: UserId): EitherNel<UserError, User> = either {
            zipOrAccumulate(
                { ensure(id.value.startsWith("ABC-")) { UserError.IdPrefixError(id) } },
                { ensure(id.value.substring(4).toInt() > 0) { UserError.IdTooLowError(id) } },
                { ensure(id.value.substring(4).toInt() < 1000) { UserError.IdTooHighError(id) } }
            ) { _, _, _ -> User(id) }
        }

        assertEquals(
            nonEmptyListOf(
                UserError.IdPrefixError(UserId("QWE-11111")),
                UserError.IdTooHighError(UserId("QWE-11111"))
            ).left(), findUser(UserId("QWE-11111"))
        )
    }

    @JvmInline
    value class UserId(val value: String)

    data class UserNotFound(val message: String)

    data class User(val userId: UserId)

    object Problem

    sealed interface UserError {
        data class IdPrefixError(val id: UserId) : UserError
        data class IdTooLowError(val id: UserId) : UserError
        data class IdTooHighError(val id: UserId) : UserError
    }
}