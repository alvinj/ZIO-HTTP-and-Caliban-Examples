package http_110b
// use sbt

import zio.*
import zio.http.*
import zio.json.*

/** 
 * EXAMPLE: Create a basic REST API using ZIO HTTP.
 *
 * PURPOSE: (1) Build on the last example by creating the database/datastore
 *          as a SERVICE and a ZLayer.
 *          (2) Also, there are several parts of the code that can and need
 *          to be refactored, because they’re a little hard to read. I’m
 *          leaving these as an exercise for the reader/viewer.
 *          (3) I do show a little logging using `tap` and `ZIO.log*`
 *          in the “delete” route.
 * 
 * After this, things you’ll want to do in the real world:
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
curl -i http://localhost:8080/todos/1

DELETE ONE:
-----------
curl -X DELETE http://localhost:8080/todos/1
curl -v -X DELETE http://localhost:8080/todos/1

*/


case class ToDo(
    id: String,
    task: String,
    completed: Boolean
) derives JsonEncoder, JsonDecoder



// we want to create our mock database as a service,
// so start with a trait:
trait TodoRepository:
    def getAll(): Task[Map[String, ToDo]]
    def get(id: String): Task[Option[ToDo]]
    def create(todo: ToDo): Task[Unit]
    def delete(id: String): Task[Boolean]

// then create an implementation of that trait for each environment.
// here’s one for a DEV environment:
class DevTodoRepository(ref: Ref[Map[String, ToDo]]) extends TodoRepository:

    def getAll(): Task[Map[String, ToDo]] =
         ref.get
         // use this to test `response.fromThrowable` below:
         // ZIO.fail(new RuntimeException("Simulated database error"))

    def get(id: String): Task[Option[ToDo]] =
        ref.get.map(map => map.get(id))

    def create(todo: ToDo): Task[Unit] =
        ref.update(map => map + (todo.id -> todo))

    def delete(id: String): Task[Boolean] =
        ref.modify { todos =>
            val newTodos = todos - id
            val found = todos.contains(id)
            (found, newTodos)
        }

object DevTodoRepository:
    // create a value as a ZLayer for dependency injection.
    // (see `program.provide` later in the code)
    val live: ZLayer[Any, Nothing, TodoRepository] = ZLayer.fromZIO {
        val ref = Ref.make(Map.empty[String, ToDo]) // a new Ref with an empty Map
        ref.map(ref => new DevTodoRepository(ref)) // create the DevRepo with the Ref
    }
    // `ZLayer.fromZIO` converts the ZIO effect into a ZLayer



object BasicRestApiWithService extends ZIOAppDefault:

    // TODO: Exercise for the reader: Create individual values for each of the
    // routes that I show here inside the Routes constructor.
    def makeRoutes(repo: TodoRepository): Routes[Any, Response] = Routes(

        // purpose: get all ToDo values in the repository.
        // in this code:
        // `map` transforms the success channel (REA’s 3rd type parameter), and
        // `mapError` transforms the error channel (REA’s 2nd type parameter)
        Method.GET / "todos" -> handler { (_: Request) =>
            repo.getAll()
                .mapError(Response.fromThrowable)   // if the operation succeeds, this has no effect.
                .map { todos =>                     // this only runs if no error occurred.
                    if todos.isEmpty then
                        Response.status(Status.NoContent)
                    else
                        Response.json(todos.values.toList.toJsonPretty)
                }
        },

        // get a single todo value by its `id`:
        Method.GET / "todos" / string("id") -> handler { (id: String, _: Request) =>
            repo.get(id)
                .mapError(Response.fromThrowable)
                .map { maybeToDo =>
                    maybeToDo match
                        case Some(todo) => Response.json(todo.toJsonPretty)
                        case None => Response.status(Status.NoContent)
                }
        },

        // ---------------------------------
        // purpose: create a new ToDo entry.
        // ---------------------------------
        // OPTION 1
        // --------
        Method.POST / "todos" -> handler { (req: Request) =>
            (for
                body <- req.body.asString
                todo <- ZIO.fromEither(body.fromJson[ToDo]).mapError(new RuntimeException(_))
                _    <- repo.create(todo)
            yield
                Response.json(todo.toJsonPretty)
                        .status(Status.Created))
                        .mapError(Response.fromThrowable)
        },

        // --------
        // OPTION 2
        // --------
        // here i convert all the errors to Strings, so i use `catchAll` in
        // the end instead ot `mapError`. If you want to use `mapError`,
        // make sure each line in the for-expression returns a Throwable.
//        Method.POST / "todos" -> handler { (req: Request) =>
//            (for
//                body <- req.body
//                           .asString
//                           .mapError(e => s"Body read error: ${e.getMessage}")
//                todo <- ZIO.fromEither(body.fromJson[ToDo])
//                           .mapError(_.toString)
//                _    <- repo.create(todo)
//                            .mapError(e => s"Database error: ${e.getMessage}")
//            yield
//                Response.json(todo.toJsonPretty)
//                        .status(Status.Created))
//                        .catchAll { error =>
//                            ZIO.succeed(
//                                Response.json(error.toString)
//                                    .status(Status.BadRequest)
//                            )
//                        }
//        },

        // Delete a ToDo.
        // Also, start to show how to add logging with this style of code.
        Method.DELETE / "todos" / string("id") -> handler { (id: String, _: Request) =>
            repo.delete(id)                         // returns Task[Boolean]
                .tap(exists => ZIO.logInfo(s"DELETE: id=$id, value exists=$exists"))
                .tapError(error => ZIO.logError(s"DELETE: id=$id, error=${error.getMessage}"))
                .mapError(Response.fromThrowable)   // converts Throwable to Response
                .map { exists =>                    // maps the Boolean to a Response
                    if exists then
                        // this is one way of handling “the delete operation worked” case:
                        Response.status(Status.NoContent)
                    else
                        // this is one way to handle the case where the `id` is not
                        // found in the datastore:
                        Response(
                            status = Status.NotFound,
                            body = Body.fromString(s"Todo with id '$id' not found")
                        )
                }
        }

    ) //end of Routes(...)

    val program = for
        repo <- ZIO.service[TodoRepository]   // note that this references the trait
        app   = makeRoutes(repo)
        _    <- Server.serve(app)
    yield ()

    // i just keep this to remind you that you can configure these things
    val serverConfig = Server.Config.default
        .port(8080)               // change port, if desired
        .keepAlive(true)          // keep TCP connections open for multiple requests
        .idleTimeout(30.seconds)  // close connection if no data received for 30 seconds    

    val run = program.provide(
        ZLayer.succeed(serverConfig),
        DevTodoRepository.live,   // our DEV repo
        Server.live
    )




