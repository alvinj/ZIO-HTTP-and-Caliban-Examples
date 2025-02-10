package http_106
// use sbt

import zio.*
import zio.http.*
import zio.json.*

/**
 * Purpose:
 *     - Show how to add logging messages to the previous example.
 *     - Show why/where ZIO.succeed is needed.
 * 
 * TEST WITH:
 *      curl http://localhost:8080/hi
 *      curl http://localhost:8080/hello
 *      curl http://localhost:8080/hello/alvin
 * 
 * NOTE:
 *      changes to log messages don’t seem to work
 *      well with `scala-cli *scala --watch`
 */
object PathParamsAndJson extends ZIOAppDefault:

    // can add ", JsonDecoder" when needed
    case class Greeting(message: String) derives JsonEncoder

    val hi: Route[Any, Nothing] = Method.GET / "hi" -> handler { (_: Request) =>
        Response.text("hi")
    }

    val hello = Method.GET / "hello" -> handler { (_: Request) =>
        // implement logging with a for-expression
        val zResponse: ZIO[Any, Nothing, Response] = for
            _        <- ZIO.log("GET /hello called")
            response <- ZIO.succeed(Response.json(Greeting("Hello, World!").toJson))
        yield
            response
        zResponse
    }

    val helloName = Method.GET / "hello" / string("name") -> handler { (name: String, request: Request) =>
        // implement logging with *>  (“and then”)
        val zResponse = ZIO.log("GET /hello/NAME called") *>
            ZIO.succeed(Response.json(Greeting(s"Hello, $name!").toJson))
               .tap(response => ZIO.log(s"TAPPED VALUE: $response"))
        zResponse
    }

    val app = Routes(
        hi, hello, helloName
    )

    val run = Server
        .serve(app)
        .provide(Server.default)

