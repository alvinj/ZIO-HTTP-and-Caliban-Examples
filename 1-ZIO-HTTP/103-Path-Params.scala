package alvin_103
// start using sbt now

import zio.*
import zio.http.*
import zio.json.*

/**
 * Example 3: Working with Path Parameters:
 *     - shows a static route
 *     - shows a dynamic route
 *
 * NOTE:
 *     - start using sbt now because it makes dev much simpler
 *     - look at build.sbt
 *
 * TEST WITH:
 *      curl http://localhost:8080/hello
 *      curl http://localhost:8080/hello/alvin
 *      curl http://localhost:8080/users/101
 */
object PathParams extends ZIOAppDefault:

    // our app now has multiple routes
    val app = Routes(

        // static path
        Method.GET / "hello" -> handler { (_: Request) =>
            Response.text("Hello, world!")
        },

        // dynamic path parameter (string)
        Method.GET / "hello" / string("name") -> handler { (name: String, request: Request) =>
            Response.text(s"Hello, $name!")
        },

        // dynamic path parameter (int)
        Method.GET / "users" / int("id") -> handler { (id: Int, request: Request) =>
            Response.text(s"ID: $id")
        },

        // multiple dynamic path parameters
        Method.GET / "catalog" / string("category") / string("subcategory") / int("page") / int("limit") -> handler {
            (category: String, subcategory: String, page: Int, limit: Int, request: Request) =>
                Response.text("Hi there")
        }


        // see docs for more info: https://zio.dev/zio-http/reference/routing/route_pattern
    )

    // (similar to the previous example)
    val serverConfig = Server.Config.default
        .port(8080)
        .keepAlive(true)
        .idleTimeout(30.seconds)

    // (similar to the previous example)
    val run = Server
        .serve(app)
        .provide(
            ZLayer.succeed(serverConfig),
            Server.live
        )
