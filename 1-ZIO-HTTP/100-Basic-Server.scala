//> using scala "3"
//> using dep "dev.zio::zio::2.1.13"
//> using dep "dev.zio::zio-http::3.0.1"
package http_100

import zio.*
import zio.http.*

/**
 * ---------
 * ZIO HTTP:
 * ---------
 *
 * - Built on top of ZIO, leveraging its power for managing effects, fibers, &
 *   concurrency.
 *
 * - Also built on top of Netty (Java NIO, non-blocking I/O). Netty is used by Apple,
 *   Google, Twitter, Facebook, Square, Instagram, etc.
 *
 * - “ZIO HTTP is designed in terms of HTTP as function, where both server and client are
 *   a function from a request to a response, with a focus on type safety, composability,
 *   and testability.”
 *
 * - All operations are asynchronous and non-blocking by default.
 *
 * - Minimal boilerplate, with a declarative and intuitive DSL.
 *
 * - Provides a Request object to handle incoming data (headers, query params, body),
 *   and a Response object to construct outgoing messages.
 *
 * - Routing is declarative, functional, and pattern-based. You can match on methods,
 *   paths, and headers.
 *
 * - ZIO HTTP supports typed error channels, allowing fine-grained control over
 *   error handling.
 *
 * - Includes “middleware” for common use-cases like logging, authentication,
 *   and compression. (Middleware is a composable way to wrap HTTP routes with
 *   additional behavior. Look for the @@ symbol in future examples.)
 *
 * - Has built-in WebSocket support for bi-directional communication.
 *
 * - Server configuration with ZLayer (dependency injection).
 *
 * - Can serve static files directly from the filesystem.
 *
 * - Integrates seamlessly with other ZIO libraries.
 *
 * Documentation Pages To Show:
 *
 * - https://zio.dev/zio-http/
 * - https://zio.dev/zio-http/reference/aop/middleware/
 * - https://zio.dev/zio-http/reference/handler
 *
 * ----------
 * Example 1:
 * ----------
 *
 * - A “Hello, world” ZIO HTTP server.
 * - Then add some changes (below) to show more Route details.
 * - Changes also show Response.text and Response.json
 *
 * TEST:
 *      curl http://localhost:8080/hello    # v1, v2
 *      curl http://localhost:8080/json     # v2
 *      curl http://localhost:8080/         # v2
 *
 *      curl -i ...
 *      curl -v ...
 *
 * ALSO: See the “Version 2” notes at the bottom for
 *       those changes/additions about Route, handler,
 *       and Response.
 */
object FirstServer extends ZIOAppDefault:

    // --------------------------------------------
    // [1] simplest "hello, world" example (almost)
    // --------------------------------------------
    val helloRoute =
        Method.GET / "hello" -> handler(Response.text("Hello, ZIO HTTP world"))

    val app = Routes(helloRoute)

    val run = Server
        .serve(app)
        .provide(Server.default)


    /**
     *  THINGS TO KNOW:
     *
     * - from the `Routes` docs: “An HTTP application is a collection of routes, 
     *   all of whose errors have been handled through conversion into 
     *   HTTP responses.”
     * 
     * - `Method.GET / "hello"` creates a `Route`
     * 
     * - the `->` symbol creates a two-element tuple (also known as a 
     *   key/value pair or a mapping)(see below for more)
     * 
     * - `handler()` creates a request handler
     * 
     * - `Server.serve(app)` starts the HTTP server with the defined routes
     * 
     * - `.provide(Server.default)` injects the default server dependencies
     * 
     * - runs on port 8080 by default
     * 
     */


    // -----------------------------------------
    // [2] show each route as a separate effect.
    //     also, add a json route.
    // -----------------------------------------

    // can run in Any environment and cannot fail (Nothing):
//     val helloRoute =
//         Method.GET / "hello" -> handler(Response.text("Hello route!"))  // Content-Type: text/plain
//
//     val homeRoute: Route[Any, Nothing] =
//         Method.GET / Root -> handler(Response.text("Home route!"))
//
//     val jsonRoute =
//         Method.GET / "json" -> handler(
//             Response.json("""{"greeting": "Hello, json world"}""")   // application/json
//         )
//
//     val routes = Routes(
//         homeRoute, helloRoute, jsonRoute
//     )
//
//     override val run = Server
//         .serve(routes)
//         .provide(Server.default)



