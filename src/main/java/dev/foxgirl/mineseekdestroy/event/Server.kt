package dev.foxgirl.mineseekdestroy.event

import io.javalin.Javalin
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.Filter
import org.apache.logging.log4j.core.LogEvent
import org.apache.logging.log4j.core.Logger
import org.apache.logging.log4j.core.filter.AbstractFilter

fun start() {

    (LogManager.getRootLogger() as Logger).addFilter(object : AbstractFilter() {
        override fun filter(event: LogEvent): Filter.Result {
            if (event.level.isMoreSpecificThan(Level.INFO)) {
                Bus.publish(MessageEvent(event.message.formattedMessage))
            }
            return Filter.Result.NEUTRAL
        }
    })

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
