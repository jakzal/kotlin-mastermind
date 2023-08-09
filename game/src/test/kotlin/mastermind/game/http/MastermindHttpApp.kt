package mastermind.game.http

import mastermind.game.acceptance.DecodingBoard
import org.http4k.core.*
import org.http4k.format.Jackson.auto
import org.http4k.lens.Path
import org.http4k.routing.bind
import org.http4k.routing.routes

fun mastermindHttpApp() = routes(
    "/games" bind Method.POST to { _: Request ->
        Response(Status.CREATED).header("Location", "/games/6e252c79-4d02-4b05-92ac-6040e8c7f057")
    },
    "/games/{id}" bind Method.GET to { request ->
        val id = Path.of("id")(request)
        Response(Status.OK).with(
            Body.auto<DecodingBoard>().toLens() of DecodingBoard(id, 4, 12, emptyList(), "In progress")
        )
    }
)