package http_112c

import zio.*
import zio.http.Middleware.basicAuth
import zio.http.*
import zio.http.codec.PathCodec.string

/**

PURPOSE:

1. This example works, and uses a `basicAuth` Middleware function
   that is in the ZIO HTTP `HandlerAspect` class.

2. I created a route that DOES require Basic Auth,
   and other routes that DO NOT require authentication.

3. To be clear, `basicAuth` is a “Middleware” function.
   Middleware is like AOP.

 # works without auth:
 curl -i http://localhost:8080/health
 curl -i http://localhost:8080/version

 # GET all todos with Basic Auth
 curl -u admin:admin http://localhost:8080/user/alvin/greet

 */


// BASED ON:
// https://github.com/zio/zio-http/blob/main/zio-http-example/src/main/scala/example/BasicAuth.scala
object BasicAuthMiddleware extends ZIOAppDefault:

    // ----------------------------
    // [1] individual public routes
    // ----------------------------
    val healthRoute = Routes(
        Method.GET / "health" -> Handler.text("OK")
    )

    val versionRoute: Routes[Any, Response] = Routes(
        Method.GET / "version" ->
            handler { (_: Request) =>
                Response.text("v1.0.0")
            }
    )

    // ------------------------------------------------
    // [2] protected routes that require authentication
    // ------------------------------------------------
    val protectedRoutes: Routes[Any, Response] = Routes(
        Method.GET / "user" / string("name") / "greet" ->
            handler { (name: String, _: Request) =>
                Response.text(s"Welcome to the ZIO party! ${name}")
            }
    )

    // --------------------------
    // [3] combined public routes
    // --------------------------
    val publicRoutes: Routes[Any, Response] =
        healthRoute ++ versionRoute

    // add `basicAuth` middleware only to protected routes
    val authenticatedRoutes: Routes[Any, Response] =
        protectedRoutes @@ basicAuth("admin", "admin")   // <== HUGE!!!

    // combine all routes
    val allRoutes: Routes[Any, Response] =
        authenticatedRoutes ++ publicRoutes

    val run = Server.serve(allRoutes)
                    .provide(Server.default)





