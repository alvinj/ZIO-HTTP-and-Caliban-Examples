package http_112a

import zio.*
import zio.http.*
import zio.json.*

/**

 PURPOSE: This is very similar to the previous example, but starts to
          use something of a “middleware” approach to solve the problem.
          (where middleware != Middleware)

 # a public route that works without auth:
 curl -i http://localhost:8080/health

 # GET all todos with Basic Auth
 curl -iu admin:password http://localhost:8080/todos

 # CREATE one todo with Basic Auth
 curl -u admin:password -X POST http://localhost:8080/todos \
      -H "Content-Type: application/json" \
      -d '{"id": "1", "task": "Learn ZIO", "completed": false}'

 # verbose failure:
 curl -v http://localhost:8080/todos

 # info failure:
 curl -i http://localhost:8080/todos

 */

object AuthHelperFunction extends ZIOAppDefault:

    case class ToDo(id: String, task: String, completed: Boolean) derives JsonEncoder, JsonDecoder
    case class User(username: String)
    case class Credentials(username: String, password: String) derives JsonEncoder, JsonDecoder
    type ToDoDb = Ref[Map[String, ToDo]]

    import scala.util.control.Exception.allCatch
    private def decodeBase64(s: String): Option[Array[Byte]] =
        allCatch.opt(java.util.Base64.getDecoder().decode(s))

    /**
     * Validates “Basic Authentication” credentials from the request header.
     * Expects "admin:password" credentials in base64 encoded format.
     * @param request The incoming HTTP request
     * @return true if credentials are valid, false otherwise
     */
    def isAuthenticated(request: Request): Boolean =
        val maybeAuthenticated = for
            // `if authHeader.startsWith("Basic ")` is a filter condition:
            authHeader <- request.headers.get("Authorization") if authHeader.startsWith("Basic ")
            decoded    <- decodeBase64(authHeader.substring(6))
            credentials               = String(decoded)
            Array(username, password) = credentials.split(":")
        yield
            username == "admin" && password == "password"   // get these from a data store
        maybeAuthenticated.getOrElse(false)

    // OLDER, but maybe easier to read:
//    def isAuthenticated(request: Request): Boolean =
//        request.headers.get("Authorization") match
//            case Some(auth) if auth.startsWith("Basic ") =>
//                try
//                    val decoded = java.util.Base64.getDecoder().decode(auth.substring(6))
//                    new String(decoded).split(":") match
//                        case Array(username, password) =>
//                            username == "admin" && password == "password"
//                        case _ => false
//                catch
//                    case _: Exception => false
//            case _ => false


    /**
     * Authentication middleware that wraps route handlers.
     * Returns 401 Unauthorized if authentication fails.
     * @param handler The route handler to protect. Note that this is a function
     *                that transforms a Request into a UIO[Response].
     * @return A wrapped handler that checks authentication before proceeding.
     *         Note that this is also a function that transforms a
     *         Request => UIO[Response]. Because these signatures match,
     *         withAuth can be used wherever a `Request => UIO[Response]`
     *         is needed. The returned function essentially wraps the
     *         `handler` input function with this authentication capability.
     */
    private def withAuth(
        handler: Request => ZIO[Any, Nothing, Response]
    ): Request => ZIO[Any, Nothing, Response] =
        (req: Request) =>
            if !isAuthenticated(req) then
                ZIO.succeed(
                    Response.status(Status.Unauthorized)
                        .addHeader("WWW-Authenticate", "Basic realm=\"Todo API\"")
                )
            else
                handler(req)

    /**
     * Creates all routes for the ToDo API.
     * Includes public health check and protected CRUD operations.
     *
     * @param db Reference to the in-memory ToDo database
     * @return Combined routes for the API
     */
    def makeRoutes(db: ToDoDb): Routes[Any, Response] =

        // public health check endpoint; no authentication required
        val healthRoute = Method.GET / "health" -> handler { (_: Request) =>
            Response.text("OK")
        }

        // lists all ToDos for authenticated users
        val listTodosRoute = Method.GET / "todos" -> handler { (req: Request) =>
            withAuth(_ =>
                for
                    todos <- db.get
                yield
                    if todos.isEmpty then Response.status(Status.NoContent)
                    else Response.json(todos.values.toList.toJson)
            )(req)
        }

        // retrieves a specific ToDo by ID
        val getTodoRoute = Method.GET / "todos" / string("id") -> handler { (id: String, req: Request) =>
            withAuth(_ =>
                for
                    todos <- db.get
                yield todos.get(id) match
                    case Some(todo) => Response.json(todo.toJson)
                    case None => Response.status(Status.NoContent)
            )(req)
        }

        // creates a new ToDo from JSON request body
        val createTodoRoute = Method.POST / "todos" -> handler { (req: Request) =>
            withAuth(req =>
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
            )(req)
        }

        // deletes a ToDo by ID
        val deleteTodoRoute = Method.DELETE / "todos" / string("id") -> handler { (id: String, req: Request) =>
            withAuth(_ =>
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
            )(req)
        }

        Routes(
            healthRoute,
            listTodosRoute,
            getTodoRoute,
            createTodoRoute,
            deleteTodoRoute
        )

    // main program that initializes the database and starts the HTTP server
    val program = for
        db  <- Ref.make(Map.empty[String, ToDo])
        app  = makeRoutes(db)
        _   <- Server.serve(app)
    yield ()

    // entry point that provides the server implementation
    val run =
        program.provide(Server.default)








