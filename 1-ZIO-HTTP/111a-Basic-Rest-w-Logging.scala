package http_111a

import zio.*
import zio.http.*
import zio.json.*
import zio.logging.{ConsoleLoggerConfig, LogFormat}
import zio.logging.LogFilter
import zio.logging.consoleLogger

/**
 * PURPOSE: The purpose of this code is to demonstrate both (a) logging,
 *          and also (b) logging in a format that I like.
 *
 * NOTES:
 *          This is a simplified version of the previous example (110b),
 *          that adds in a little logging.
 *
 GET ALL:
 --------
 curl http://localhost:8080/todos
 curl -i http://localhost:8080/todos

 */
object BasicRestApiWithLogging extends ZIOAppDefault:

    // ------------------------------------------------------------------
    // START: customize the logging output to what i want it to look like
    // ------------------------------------------------------------------
    // EXERCISE: show the class/method where the 'message' output is coming from
    val customLogFormat =
        LogFormat.timestamp + LogFormat.text(" | ") +
        LogFormat.level     + LogFormat.text(" | ") +
        LogFormat.fiberId   + LogFormat.text(" | ") +
        LogFormat.line   // our 'message'

    // EXERCISE: get "debug" output to show up
    val loggerConfig = ConsoleLoggerConfig(
        format = customLogFormat,
        filter = LogFilter.LogLevelByNameConfig(
            rootLevel = LogLevel.Info,
            mappings = Map[String, LogLevel]()
            // mappings = Map(
            //     "com.myapp.core" -> LogLevel.Debug,
            //     "com.myapp.api" -> LogLevel.Info
            // )
        )
    )

    override val bootstrap = Runtime.removeDefaultLoggers >>>
        consoleLogger(config = loggerConfig)
    // ------------------------------------------------------------------
    // END customization of logging output
    // ------------------------------------------------------------------


    // the rest of the application starts now ...
    case class ToDo(
        id: String,
        task: String,
        completed: Boolean
    ) derives JsonEncoder, JsonDecoder

    type ToDoDb = Ref[Map[String, ToDo]]


    // WITHOUT (MUCH) LOGGING
//    def makeRoutes(db: ToDoDb): Routes[Any, Response] = Routes(
//        Method.GET / "todos" -> handler { (_: Request) =>
//            for
//                _     <- ZIO.logInfo("Fetching all ToDo's ...")
//                todos <- db.get
//                // if this could fail, you’d write code like this:
//                // todos <- db.get.tapError(err => ZIO.logError(s"Failed to fetch ToDos: ${err.getMessage}"))
//            yield
//                if todos.isEmpty then
//                    Response.status(Status.NoContent)
//                else
//                    Response.json(todos.values.toList.toJsonPretty)
//        }
//    )


    // WITH LOGGING (Version 1)
    def makeRoutes(db: ToDoDb): Routes[Any, Response] = Routes(
        Method.GET / "todos" -> handler { (_: Request) =>
            for
                _        <- ZIO.logInfo("Received a request to fetch all ToDos")
                todos    <- db.get
                _        <- ZIO.logDebug(s"Fetched ToDos from the database: $todos")
                response <-
                            if todos.isEmpty then
                                ZIO.logInfo("No ToDos found, responding with 204 No Content")
                                   .as(Response.status(Status.NoContent))
                            else
                                ZIO.logInfo(s"Found ${todos.size} ToDos, responding with JSON")
                                   .as(Response.json(todos.values.toList.toJsonPretty))
            yield response
        }.mapError(_ => Response.status(Status.InternalServerError))
        // ^^^ EXERCISE: how do you implement logging here ^^^
    )

    // EXERCISE: switch to using this code and test it
    // WITH LOGGING (Version 2)
//    def makeRoutes(db: ToDoDb): Routes[Any, Response] = Routes(
//        Method.GET / "todos" -> handler { (_: Request) =>
//            for
//                _ <- ZIO.logInfo("Received a request to fetch all ToDos")
//                todos <- db.get
//                _ <- ZIO.logDebug(s"Fetched ToDos from the database: $todos")
//                response <-
//                    // [1] this `if` block must return a Response under all conditions.
//                    if todos.isEmpty then
//                        // [2] ZIO.logInfo type: ZIO[Any, Nothing, Unit].
//                        // [3] `*>` chains effects, ignoring the result of the first (Unit from ZIO.logInfo)
//                        //     and returning the result of the second (Response, from ZIO.succeed).
//                        ZIO.logInfo("No ToDos found, responding with 204 No Content") *>
//                            ZIO.succeed(Response.status(Status.NoContent))
//                    else
//                        ZIO.logInfo(s"Found ${todos.size} ToDos, responding with JSON") *>
//                            ZIO.succeed(Response.json(todos.values.toList.toJsonPretty))
//            yield response
//        }.mapError(_ => Response.status(Status.InternalServerError))
//    )


    // i created this so our initial Map is not empty
    val testTodos = Map(
        "1" -> ToDo("1", "Test task", false),
        "2" -> ToDo("2", "Another task", true)
    )

    val program = for
        _   <- ZIO.logInfo("Starting the program ...")
        db  <- Ref.make(testTodos)
        app  = makeRoutes(db)
        _   <- Server.serve(app)
    yield ()

    val serverConfig = Server.Config.default
        .port(8080)
        .keepAlive(true)
        .idleTimeout(30.seconds)

    override val run = program.provide(
        ZLayer.succeed(serverConfig),
        Server.live
    )