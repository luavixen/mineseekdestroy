package dev.foxgirl.mineseekdestroy.event

import dev.foxgirl.mineseekdestroy.Game
import io.javalin.Javalin
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

fun start() {

    val app = Javalin.create()

    app.post("/execute") { ctx ->
        val server = Game.getGame().server
        server.commandManager.executeWithPrefix(server.commandSource, ctx.body())
    }

    app.ws("/listen") { ws ->
        var subscription: Bus.Subscription? = null
        ws.onConnect { ctx ->
            subscription = Bus.subscribe { event ->
                ctx.send(Json.encodeToString(event))
            }
        }
        ws.onError { ctx ->
            ctx.closeSession()
            subscription?.unsubscribe()
        }
        ws.onClose { ctx ->
            subscription?.unsubscribe()
        }
    }

    app.start("127.0.0.1", 25566)

    Runtime.getRuntime().addShutdownHook(Thread { app.stop() })

}
