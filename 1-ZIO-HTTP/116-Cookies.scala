package http_116

import zio.*
import zio.http.*
import Middleware.signCookies
import java.net.URLEncoder
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

/**
 *
 * PURPOSE:
 * - show how to work with cookies
 *
 * NO COOKIES:     curl -i http://localhost:8080/
 * COOKIES:        curl -i http://localhost:8080/cookie
 * CLEAR COOKIES:  curl -i http://localhost:8080/clear_cookie
 *
 * GET THE INITIAL COOKIES AND SAVE IT TO A FILE:
 *     curl -v http://localhost:8080/cookie -c cookies.txt
 *
 * SEND THE COOKIES BACK WITH OUR GET REQUEST:
 *     curl -v http://localhost:8080/cookie -b cookies.txt
 *
 * NOTE:
 *     in the curl output, > signals the “outgoing” information
 *     (our request), and < signals the incoming information
 *     (the server response).
 *
 * SEE:
 *     https://zio.dev/zio-http/reference/headers/session/cookies
 *     https://github.com/zio/zio-http/blob/main/zio-http-example/src/main/scala/example/SignCookies.scala
 */
object SignCookies extends ZIOAppDefault:

    val COOKIE_SECRET = "secret"
    val COOKIE_DOMAIN = Some("alvinalexander.com")

    // cookie #1
    val COOKIE_SESSION_NAME  = "session_id"
    val COOKIE_SESSION_VALUE = "abc1234567"   // get from a db

    // cookie #2
    val COOKIE_USER_PREFS_NAME  = "user_prefs"
    val COOKIE_USER_PREFS_VALUE = encodeCookieValue("theme=dark;lang=en")

    // [1] this is a “Response Cookie”
    private val sessionCookie = Cookie.Response(
        name    = COOKIE_SESSION_NAME,                    // cookie name
        content = COOKIE_SESSION_VALUE,                   // cookie value
        // domain = COOKIE_DOMAIN,                        // can’t do this in this demo
        maxAge  = Some(5.days)                            // cookie expiration
    )

    // [2] this is a second Response Cookie:
    private val userPrefsCookie = Cookie.Response(
        name    = COOKIE_USER_PREFS_NAME,
        content = COOKIE_USER_PREFS_VALUE,
        maxAge  = Some(30.days)
    )

    /**
     * - Return an OK response with a signed cookie.
     * - `cookie.sign("secret")` cryptographically signs the cookie,
     *     using the string "secret" as the signing key.
     * - signing prevents cookie tampering since the server can verify the signature.
     */
    // val cookieRoute = Method.GET / "cookie" ->
    //     handler(Response.ok.addCookie(cookie.sign("secret")))

    val cookieRoute = Method.GET / "cookie" -> handler { (request: Request) =>
        ZIO.succeed {

            // note: here i am just looking for the "session" cookie:
            val maybeRequestSessionCookie = request.cookie(COOKIE_SESSION_NAME)

            maybeRequestSessionCookie match
                case Some(requestSessionCookie) =>

//                    val maybeReqUserPrefsCookie = request.cookie(COOKIE_USER_PREFS_NAME).get.toRequest.unSign(COOKIE_SECRET)
//                    maybeReqUserPrefsCookie.foreach(reqCookie => println(s"USER PREFS: ${reqCookie.name} = ${decodeCookieValue(reqCookie.content)}"))

                    // THE COOKIE WAS FOUND IN THE REQUEST
                    // now we know that the client gave us a session cookie.
                    // next, we need to verify it.
                    val maybeSessionReqCookie = requestSessionCookie.toRequest.unSign(COOKIE_SECRET)
                    maybeSessionReqCookie match
                        case Some(cookie) =>
                            // we were able to un-sign the cookie correctly
                            logCookieInformation(requestSessionCookie)
                            Response.ok.addCookie(requestSessionCookie.toResponse)
                        case None =>
                            // someone is trying to hack us
                            println("THE SIGNATURES **DO NOT** MATCH")
                            Response.badRequest("Hmmm .....")
                case None =>
                    // THE COOKIE WAS *NOT* FOUND IN THE REQUEST
                    println("No cookie found in request")
                    // add two cookies, and sign them here, or below in the
                    // `serve` method using Middleware:
                    // Response.ok
                    //     .addCookie(sessionCookie.sign(COOKIE_SECRET))
                    //     .addCookie(userPrefsCookie.sign(COOKIE_SECRET))
                    // all of my cookies are signed below in `serve`:
                    Response.ok
                        .addCookie(userPrefsCookie)
                        .addCookie(sessionCookie)

        }
    }

    val clearCookieRoute = Method.GET / "clear_cookie" ->
        handler(
            Response.ok.addCookie(Cookie.clear(COOKIE_SESSION_NAME))
                       .addCookie(Cookie.clear(COOKIE_USER_PREFS_NAME))
        )

    private val routes = Routes(
        cookieRoute, clearCookieRoute
    )

    val run = Server
        .serve(routes @@ signCookies(COOKIE_SECRET))
        .provide(Server.default)


    // SOME HELPER FUNCTIONS
    def logCookieInformation(cookie: Cookie): Unit =
        println("THE SIGNATURES **DO** MATCH")
        println(
            s"""Cookie details:
               |  Name:  ${cookie.name}
               |  Value: ${cookie.content}
               |""".stripMargin
        )

    def encodeCookieValue(value: String): String =
        URLEncoder.encode(value, StandardCharsets.UTF_8)

    def decodeCookieValue(value: String): String =
        URLDecoder.decode(value, StandardCharsets.UTF_8)



