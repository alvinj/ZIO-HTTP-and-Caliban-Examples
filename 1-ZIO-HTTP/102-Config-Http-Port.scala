//> using scala "3"
//> using dep "dev.zio::zio::2.1.13"
//> using dep "dev.zio::zio-http::3.0.1"
package alvin_102

import zio.*
import zio.http.*

/**
 *
 * Example 2: “Hello, world” + how to configure a few
 *            common Server configuration parameters.
 *
 * TEST:
 *      curl http://localhost:9090/hello     # works
 *      curl http://localhost:9090/          # 404
 */
object HelloPlusServerConfig extends ZIOAppDefault:

    val rootRoute = Method.GET / Root -> Handler.text("yo")  // added during the video

    val helloRoute = Method.GET / "hello" -> handler(
        Response.text("Hello, ZIO HTTP world")
    )

    // see top of docs for simple Routes example: https://zio.dev/zio-http/reference/routing/routes
    val app = Routes(
        rootRoute,
        helloRoute
    )

    // NEW #1:
    // note: here i modify some of the defaults we get from 'Server.Config.default'.
    // this just says, “i want all the defaults except for these new values.”
    val serverConfig = Server.Config.default   //<-- click here to see default values
        .port(9090)                   // custom port
        .keepAlive(true)              // keep TCP connections open for multiple requests (improves performance)
        .idleTimeout(30.seconds)      // close connection if no data received for 30 seconds
        .requestDecompression(true)   // enable Request decompression
        .maxHeaderSize(8*1024)        // 8 KB

    // NEW #2:
    val run = Server
        .serve(app)
        .provide(
            ZLayer.succeed(serverConfig),   // a ZLayer that provides our custom server configuration
            Server.live                     // Server.live requires explicit configuration
        )

    // PREVIOUS:
    // val run = Server
    //     .serve(app)
    //     .provide(Server.default)

    // NOTES:
    // `Server.default` comes with pre-configured reasonable defaults for everything
    //     - bundles all necessary dependencies
    // `Server.live` requires you to explicitly provide configuration layers
    //    - requires you to provide dependencies explicitly





