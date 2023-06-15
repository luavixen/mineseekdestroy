package dev.foxgirl.mineseekdestroy.event

import io.javalin.Javalin
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

fun start() {

    val app = Javalin.create()

    app.ws("/") { ws ->
        var subscription: Bus.Subscription? = null
        ws.onConnect { ctx ->
            subscription = Bus.subscribe { event -> ctx.send(Json.encodeToString(event)) }
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
