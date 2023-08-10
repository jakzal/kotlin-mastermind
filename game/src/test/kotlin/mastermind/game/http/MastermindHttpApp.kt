package mastermind.game.http

import mastermind.game.GameId
import mastermind.game.acceptance.DecodingBoard
import org.http4k.core.*
import org.http4k.format.Jackson.auto
import org.http4k.lens.Path
import org.http4k.routing.bind
import org.http4k.routing.routes

interface MastermindApp {
    fun joinGame(): GameId
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
        Response(Status.OK).with(
            Body.auto<DecodingBoard>().toLens() of DecodingBoard(id, 4, 12, emptyList(), "In progress")
        )
    }
)