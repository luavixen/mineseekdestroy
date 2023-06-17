package dev.foxgirl.mineseekdestroy.event

import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.ServerConnector
import org.eclipse.jetty.servlet.ServletContextHandler
import org.eclipse.jetty.websocket.api.WebSocketAdapter
import org.eclipse.jetty.websocket.server.JettyServerUpgradeRequest
import org.eclipse.jetty.websocket.server.JettyServerUpgradeResponse
import org.eclipse.jetty.websocket.server.JettyWebSocketServerContainer
import org.eclipse.jetty.websocket.server.config.JettyWebSocketServletContainerInitializer

fun start() {

    val server = Server()
    val connector = ServerConnector(server).also { server.addConnector(it) }

    connector.port = 8080

    val context = ServletContextHandler(ServletContextHandler.SESSIONS)

    context.contextPath = "/"
    server.handler = context

    JettyWebSocketServletContainerInitializer.configure(context) { _, wsContainer: JettyWebSocketServerContainer ->
        // Configure default max size
        wsContainer.maxTextMessageSize = 65535

        // Add websockets
        wsContainer.addMapping("/listen") { jettyServerUpgradeRequest: JettyServerUpgradeRequest, jettyServerUpgradeResponse: JettyServerUpgradeResponse ->
            object : WebSocketAdapter() {

            }
        }
    }

}
