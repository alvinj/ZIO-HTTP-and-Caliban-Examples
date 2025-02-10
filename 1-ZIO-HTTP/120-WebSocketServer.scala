package http_120

import zio.*
import zio.http.*
import zio.http.ChannelEvent.Read
import zio.http.codec.PathCodec.string

/**
 * TO TEST:
 *     - use my JavaFX WebSocketClient
 *     - call this url:  "ws://localhost:8080/subscriptions"
 *     - send text like "Hi mom" or anything else
 *
 * Creates a WebSocket server using ZIO HTTP with two endpoints:
 *     - `/greet/{name}`: an HTTP endpoint returning a greeting message
 *     - `/subscriptions`: a WebSocket endpoint that does a few different things
 *
 */
object WebSocketEcho extends ZIOAppDefault:

    private val socketApp: WebSocketApp[Any] =
        // creates a WebSocket handler that processes incoming messages through the channel
        Handler.webSocket { wsChannel =>
            wsChannel.receiveAll {
                case Read(WebSocketFrame.Text("FOO")) =>
                    wsChannel.send(Read(WebSocketFrame.Text("BAR")))
                case Read(WebSocketFrame.Text("BAR")) =>
                    wsChannel.send(Read(WebSocketFrame.Text("FOO")))
                case Read(WebSocketFrame.Text(text))  =>
                    // repeat any other text 10 times
                    wsChannel.send(Read(WebSocketFrame.Text(text))).repeatN(10)
                case _ =>
                    // for non-text messages, do nothing
                    ZIO.unit
              }
        }

    private val routes: Routes[Any, Response] =
        Routes(
            // regular HTTP endpoint that returns a greeting
            Method.GET / "greet" / string("name") -> handler { (name: String, _: Request) =>
                Response.text(s"Greetings {$name}!")
            },
            // WebSocket endpoint that handles the WebSocket connections
            Method.GET / "subscriptions" -> handler(socketApp.toResponse),
        )

    override val run =
        Server.serve(routes)
              .provide(Server.default)


