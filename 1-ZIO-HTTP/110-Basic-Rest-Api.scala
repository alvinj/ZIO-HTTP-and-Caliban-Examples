package http_110
// use sbt

import zio.*
import zio.http.*
import zio.json.*

/** 
 * 
 * PURPOSE(s):
 *   - Create a basic REST API.
 *   - Shows handling different HTTP methods and request/response data.
 *   - Uses `Ref` to create a mock database.
 *   - `curl` commands to test the API are shown below.
 * 
 * Things you’ll want to do in the real world:
   - add validation
   - add logging

ADD 2 ENTRIES:
--------------
curl -X POST http://localhost:8080/todos \
     -H "Content-Type: application/json" \
     -d '{"id": "1", "task": "Learn ZIO", "completed": false}'

curl -X POST http://localhost:8080/todos \
     -H "Content-Type: application/json" \
     -d '{"id": "2", "task": "Learn ZIO HTTP", "completed": false}'

GET ALL:
--------
curl http://localhost:8080/todos
curl -i http://localhost:8080/todos

GET ONE:
--------
curl http://localhost:8080/todos/1
curl http://localhost:8080/todos/2
curl -i http://localhost:8080/todos/3      # note the return code

DELETE ONE:
-----------
curl -X DELETE http://localhost:8080/todos/1
curl -i -X DELETE http://localhost:8080/todos/1

*/
object BasicRestApiWithSeparateRouteDefns extends ZIOAppDefault:

    case class ToDo(
        id: String, 
        task: String,
        completed: Boolean
    ) derives JsonEncoder, JsonDecoder

    // type alias for our mock database
    type ToDoDb = Ref[Map[String, ToDo]]

    /**
     * Route: List all ToDo values in our datastore.
     */
    def listTodosRoute(db: ToDoDb): Route[Any, Response] =
        Method.GET / "todos" -> handler { (_: Request) =>
            // use `map` to transform `db.get` into a `Response`:
            db.get.map { todos =>
                if todos.isEmpty then
                    Response.status(Status.NoContent)   // "204 No Content"
                else
                    Response.json(todos.values.toList.toJsonPretty)
            }
        }

    /**
     * Route: Get one ToDo value based on the given `id` (if it exists).
     */
    def getTodoRoute(db: ToDoDb): Route[Any, Response] =
        Method.GET / "todos" / string("id") -> handler { (id: String, _: Request) =>
            db.get.map { todos =>
                todos.get(id) match
                    case Some(todo) => Response.json(todo.toJsonPretty)   // use `toJson` in the real world
                    case None => Response.status(Status.NoContent)
            }
        }

    /**
     * Route: Create a new ToDo value and save it in our DataStore.
     */
    def createTodoRoute(db: ToDoDb): Route[Any, Response] =
        Method.POST / "todos" -> handler { (req: Request) =>
            (for
                body <- req.body.asString
                todo <- ZIO.fromEither(body.fromJson[ToDo])
                _    <- db.update(db => db + (todo.id -> todo))
                // _    <- db.update(_ + (todo.id -> todo))   // alternate syntax
            yield Response.json(todo.toJsonPretty).status(Status.Created))
                .catchAll { error =>
                    ZIO.succeed(
                        Response.text(error.toString)
                                .status(Status.BadRequest)
                    )
                }
        }

    /**
     * Route: Delete a ToDo value from the Datastore.
     * Notes:
     * - May also use "403 Forbidden" or "401 Unauthorized" with Authentication/Authorization.
     * - May also use "500 Internal Server Error" for other errors.
     */
    def deleteTodoRoute(db: ToDoDb): Route[Any, Response] =
        Method.DELETE / "todos" / string("id") -> handler { (id: String, _: Request) =>
            deleteTodoById(db, id)
                .map {
                    case true  =>
                        // could also return a 200 code with a message
                        Response.status(Status.NoContent)
                    case false =>
                        Response(
                            status = Status.NotFound,
                            body = Body.fromString(s"ToDo with id '$id' not found")
                        )
                }
        }

    // returns `true` if the entry is deleted.
    private def deleteTodoById(db: ToDoDb, id: String): UIO[Boolean] =
        db.modify { todos =>
            val updatedTodos = todos - id       // Remove the ToDo from the map
            val wasDeleted = todos.contains(id) // Check if the ToDo existed
            (wasDeleted, updatedTodos)          // Return whether it was deleted
        }

    // create our `Routes` definition
    def makeRoutes(db: ToDoDb): Routes[Any, Response] =
        Routes(
            listTodosRoute(db),
            getTodoRoute(db),
            createTodoRoute(db),
            deleteTodoRoute(db)
        )

    val program = for
        db      <- Ref.make(Map.empty[String, ToDo])
        routes   = makeRoutes(db)
        _       <- Server.serve(routes)
    yield ()

    // i just keep this to remind you that you can configure these things
    val serverConfig = Server.Config.default
        .port(8080)               // change port, if desired
        .keepAlive(true)          // keep TCP connections open for multiple requests
        .idleTimeout(30.seconds)  // close connection if no data received for 30 seconds    

    val run = program.provide(
        ZLayer.succeed(serverConfig),
        Server.live
    )

