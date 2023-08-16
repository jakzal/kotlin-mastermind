package mastermind.game.journal

import arrow.core.Either
import arrow.core.NonEmptyList
import mastermind.game.CommandHandler
import mastermind.game.journal.Stream.UpdatedStream

context(Journal<EVENT>)
class JournalCommandHandler<COMMAND : Any, EVENT : Any, FAILURE : Any, RESULT>(
    private val execute: Execute<COMMAND, EVENT, FAILURE>,
    private val streamNameResolver: (COMMAND) -> String,
    private val calculateResult: (NonEmptyList<EVENT>) -> RESULT
) : CommandHandler<COMMAND, JournalFailure<FAILURE>, RESULT> {
    override suspend operator fun invoke(command: COMMAND): Either<JournalFailure<FAILURE>, RESULT> {
        val streamName = streamNameResolver(command)
        return stream(streamName) {
            execute(command).map {
                UpdatedStream(streamName, 0L, emptyList(), it)
            }
        }.map {
            calculateResult(it.events)
        }
    }
}