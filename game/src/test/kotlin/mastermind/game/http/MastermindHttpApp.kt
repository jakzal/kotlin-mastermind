package mastermind.game.http

import mastermind.game.GameId
import mastermind.game.DecodingBoard
import org.http4k.core.*
import org.http4k.format.Jackson.auto
import org.http4k.lens.Path
import org.http4k.routing.bind
import org.http4k.routing.routes

interface MastermindApp {
    fun joinGame(): GameId
    fun viewDecodingBoard(id: String): DecodingBoard?
}

fun mastermindHttpApp(
    app: MastermindApp
) = routes(
    "/games" bind Method.POST to { _: Request ->
        app.joinGame().let { gameId ->
            Response(Status.CREATED).header("Location", "/games/${gameId.value}")
        }
    },
    "/games/{id}" bind Method.GET to { request ->
        val id = Path.of("id")(request)
        app.viewDecodingBoard(id)?.let {
            Response(Status.OK).with(
                Body.auto<DecodingBoard>().toLens() of it
            )
        }?: throw RuntimeException("TODO")
    }
)