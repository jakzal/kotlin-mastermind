package mastermind.eventsourcing

import arrow.core.Either
import arrow.core.NonEmptyList
import arrow.core.toNonEmptyListOrNull

class NoStateHandler<COMMAND : Any, EVENT : Any, ERROR : Any>(
    private val execute: Execute<COMMAND, NonEmptyList<EVENT>?, ERROR, EVENT>,
) : Execute<COMMAND, List<EVENT>, ERROR, EVENT> {
    override fun invoke(command: COMMAND, events: List<EVENT>): Either<ERROR, NonEmptyList<EVENT>> {
        return execute(command, events.toNonEmptyListOrNull())
    }
}