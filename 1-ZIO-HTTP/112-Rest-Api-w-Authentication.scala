package http_112

import zio.*
import zio.http.*
import zio.json.*

/**
 *
 * PURPOSE: Show how to implement Basic Auth in a ZIO HTTP application.
 *          Don’t do anything too fancy, just show at a low level
 *          what needs to be done.
 *          (Later I’ll implement this using a ZIO HTTP Middleware approach.)
 *
 * NOTES:
 *          - Authentication == Who are you?
 *          - Authorization  == What are you allowed to do?

-----

Basic Authentication (Basic Auth) is a simple HTTP authentication scheme
where credentials are sent as base64-encoded "username:password" in
the Authorization header:

    Authorization: Basic dXNlcm5hbWU6cGFzc3dvcmQ=

Key points:

- Credentials are only encoded, not encrypted; must be used with HTTPS
- Browser shows a built-in login popup when server requests Basic Auth
- Server indicates auth is required via 401 status code and "WWW-Authenticate" header
- Credentials are sent with every request

-----


# try WITHOUT using Basic Auth (this will fail)
curl -i http://localhost:8080/todos

# GET all todos WITH Basic Auth
curl -u admin:password http://localhost:8080/todos

# CREATE one todo with Basic Auth (username=admin, password=password)
curl -u admin:password \
     -X POST http://localhost:8080/todos \
     -H "Content-Type: application/json"  \
     -d '{"id": "3", "task": "Learn ZIO", "completed": false}'

# verbose failure:
curl -v http://localhost:8080/todos

# info failure:
curl -i http://localhost:8080/todos

 * 
 */


/**
 * A RESTful API implementation with basic authentication for managing ToDo items.
 * Provides CRUD operations for ToDo’s with “Basic Auth” authentication.
 */
object BasicAuthenticatedApi extends ZIOAppDefault:

    // represents a to-do item
    case class ToDo(
        id: String,
        task: String,
        completed: Boolean
    ) derives JsonEncoder, JsonDecoder

    // represents a system user
    case class User(username: String)

    // credentials used for authentication
    case class Credentials(username: String, password: String) derives JsonEncoder, JsonDecoder

    // type alias for the in-memory database storing ToDo items
    type ToDoDb = Ref[Map[String, ToDo]]

    // send this response to unauthenticated users
    val unauthorizedResponse = ZIO.succeed(
        Response.status(Status.Unauthorized)
                .addHeader("WWW-Authenticate", """Basic realm="ToDo API" """)
    )

    import scala.util.control.Exception.allCatch
    private def decodeBase64(s: String): Option[Array[Byte]] =
        allCatch.opt(java.util.Base64.getDecoder().decode(s))

    /**
     * Validates Basic Authentication credentials from the request header.
     * Note that `auth` will contain something like "Basic <base64-credentials>",
     *     where the base64 part may vary, because it is the Base64-encoded
     *     credentials.
     *     The spec (RFC 7617) only requires checking the "Basic ".
     * @param request The incoming HTTP request
     * @return true if credentials match admin:password, false otherwise
     */
//    def isAuthenticated(request: Request): Boolean =
//        val maybeAuthenticated = for
//            // `if authHeader.startsWith("Basic ")` is a filter condition:
//            authHeader <- request.headers.get("Authorization") if authHeader.startsWith("Basic ")
//            decoded    <- decodeBase64(authHeader.substring(6))
//            credentials               = String(decoded)
//            Array(username, password) = credentials.split(":")
//        yield
//            username == "admin" && password == "password"   // get these from a data store
//        maybeAuthenticated.getOrElse(false)

    // NOTE: `authHeader.startsWith` may be more clear with this approach.
    def isAuthenticated(request: Request): Boolean =
        val maybeAuthenticated = for
            authHeader <- request.headers.get("Authorization")
            if authHeader.startsWith("Basic ")                 // this might be more clear
            decoded    <- decodeBase64(authHeader.substring(6))
            credentials               = String(decoded)
            Array(username, password) = credentials.split(":")  // "username:password"
        yield
            username == "admin" && password == "password"   // get these from a data store
        maybeAuthenticated.getOrElse(false)



    /**
     * Creates HTTP routes for the ToDo API with authentication
     * @param db Reference to the in-memory ToDo database
     * @return Routes handling CRUD operations for ToDos
     *
     * Routes:
     * - GET /health: Public health check endpoint
     * - GET /todos: List all ToDos
     * - GET /todos/{id}: Get specific ToDo
     * - POST /todos: Create new ToDo
     * - DELETE /todos/{id}: Remove ToDo
     */
    def makeRoutes(db: ToDoDb): Routes[Any, Response] =

        Routes(

            // Public health check endpoint - no authentication required
            Method.GET / "health" -> handler { (_: Request) =>
                Response.text("OK")
            },

            // Returns all ToDo items for authenticated users
            Method.GET / "todos" -> handler { (req: Request) =>
                if !isAuthenticated(req) then
                    unauthorizedResponse
                else
                    for
                        todos <- db.get
                    yield
                        if todos.isEmpty then Response.status(Status.NoContent)
                        else Response.json(todos.values.toList.toJson)
            },

            // Retrieves a specific ToDo by ID for authenticated users
            Method.GET / "todos" / string("id") -> handler { (id: String, req: Request) =>
                if !isAuthenticated(req) then unauthorizedResponse
                else
                    for
                        todos <- db.get
                    yield todos.get(id) match
                        case Some(todo) => Response.json(todo.toJson)
                        case None => Response.status(Status.NoContent)
            },

            /**
             * Creates a new ToDo item for authenticated users.
             * Expects ToDo JSON in the request body.
             */
            Method.POST / "todos" -> handler { (req: Request) =>
                if !isAuthenticated(req) then unauthorizedResponse
                else
                    (for
                        body <- req.body.asString
                        todo <- ZIO.fromEither(body.fromJson[ToDo])
                        _ <- db.update(_ + (todo.id -> todo))
                    yield Response.json(todo.toJson).status(Status.Created))
                        .catchAll { error =>
                            ZIO.succeed(
                                Response.json(error.toString)
                                    .status(Status.BadRequest)
                            )
                        }
            },

            // deletes a ToDo item by ID for authenticated users
            Method.DELETE / "todos" / string("id") -> handler { (id: String, req: Request) =>
                if !isAuthenticated(req) then unauthorizedResponse
                else
                    for
                        exists <- db.modify { todos =>
                            val newTodos = todos - id
                            val found = todos.contains(id)
                            (found, newTodos)
                        }
                    yield
                        if exists then Response.status(Status.NoContent)
                        else Response(
                            status = Status.NotFound,
                            body = Body.fromString(s"Todo with id '$id' not found")
                        )
            }
        )

    val testTodos = Map(
        "1" -> ToDo("1", "Test task", false),
        "2" -> ToDo("2", "Another task", true)
    )

    // initialize the database and start the server
    val program = for
        db     <- Ref.make(testTodos)
        routes  = makeRoutes(db)
        _      <- Server.serve(routes)
    yield ()

    val run =
        program.provide(Server.default)




