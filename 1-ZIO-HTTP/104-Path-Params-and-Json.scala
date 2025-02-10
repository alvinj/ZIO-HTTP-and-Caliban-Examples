package alvin_104
// start using sbt now

import zio.*
import zio.http.*
import zio.json.*

/**
 * Example 4: Working with Path Parameters and JSON:
 *     - shows a static route
 *     - shows a dynamic route
 *     - shows JSON encoding
 *     - shows @jsonField annotation ("greeting_message" instead of "message")
 *
 * NOTE:
 *     - start using sbt now because it makes dev much simpler
 *     - look at build.sbt
 *
 * TEST WITH:
 *      curl http://localhost:8080/hello
 *      curl http://localhost:8080/hello/alvin
 */
object PathParamsAndJson extends ZIOAppDefault:

    // [1] a case class with automatic JSON encoding
    // automatically generate a JSON encoder for Greeting.
    // The JsonEncoder typeclass comes from ZIO HTTP, and
    // lets the Greeting class be automatically serialized to JSON format.
    // (See the `toJson` call below.)
    // case class Greeting(message: String) derives JsonEncoder

    // [2] note: you can customize the derivation with annotations
    // (@jsonField, @jsonExclude)
    case class Greeting(
        @jsonField("greeting_message") message: String   // changes json field name
    ) derives JsonEncoder
    //
    // see the docs for more: https://zio.dev/zio-json/configuration

    // our app now has multiple routes
    val app = Routes(

        // static path
        Method.GET / "hello" -> handler { (request: Request) =>
            Response.json(Greeting("Hello, world!").toJson)
        },

        // dynamic path parameter
        Method.GET / "hello" / string("name") -> handler { (name: String, request: Request) =>
            ZIO.debug(formatRequest(request)) *>
            ZIO.succeed(Response.json(Greeting(s"Hello, $name!").toJson))
        },

        // see docs for more info: https://zio.dev/zio-http/reference/routing/route_pattern
    )

    // for debugging
    def formatRequest(request: Request): String =
        s"""
           |Method:  ${request.method}
           |Path:    ${request.path}
           |Headers: ${request.headers.map(h => s"${h.headerName}: ${h.renderedValue}").mkString("\n  ")}
           |URI:     ${request.url}
           |Query Params: ${
               val params: QueryParams = request.url.queryParams
               if
                   params.isEmpty then "none"
               else
                   for (k, v) <- params.map yield (s"$k: $v").mkString(", ")   // TODO: RE-TEST
           }
           |Remote Address: ${request.remoteAddress.getOrElse("unknown")}
           |""".stripMargin

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
