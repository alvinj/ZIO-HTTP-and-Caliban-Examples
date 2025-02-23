
/**
 * PURPOSE: The purpose of this code is to show a ZIO HTTP + MySQL application,
 *          using the Scalikejdbc library, and also using a code-packaging
 *          approach that is scalable.
 *          I also added in a little logging to show how that can work.
 *
 * TESTING:
 *
 *      curl -i http://localhost:8080/users
 *
 */

package com.valleyprogramming.myapp.config {

    import scalikejdbc.*
    import zio.*

    object DatabaseConfig:
        def init(): Task[Unit] = ZIO.attempt {
            Class.forName("com.mysql.cj.jdbc.Driver")
            ConnectionPool.singleton(
                "jdbc:mysql://localhost:8889/zio_http",
                "root",
                "root"
            )
        }
}


package com.valleyprogramming.myapp.model {
    case class User(id: Long, name: String)
}


package com.valleyprogramming.myapp.repository {

    import com.valleyprogramming.myapp.model.User
    import scalikejdbc.*
    import zio.*

    trait UserRepository:
        def getAllUsers(): Task[List[User]]

    class UserRepositoryLive extends UserRepository:
        override def getAllUsers(): Task[List[User]] = 
            ZIO.logInfo("***** GET ALL USERS CALLED *****") *>
                ZIO.attempt {
                    DB.readOnly { implicit session =>
                        sql"select id, name from users"
                            .map(rs => User(rs.long("id"), rs.string("name")))
                            .list
                            .apply()
                    }
                }.tap(users => ZIO.logInfo(s"getAllUsers query completed, userCount = ${users.size}"))

    object UserRepository:
        val layer: ULayer[UserRepository] = ZLayer.succeed(new UserRepositoryLive())
}


package com.valleyprogramming.myapp.service {

    import com.valleyprogramming.myapp.model.User
    import com.valleyprogramming.myapp.repository.UserRepository
    import zio.*

    trait UserService:
        def getAllUsers(): Task[List[User]]

    class UserServiceLive(repository: UserRepository) extends UserService:
        override def getAllUsers(): Task[List[User]] =
            repository.getAllUsers()

    object UserService:
        val layer: URLayer[UserRepository, UserService] =
            ZLayer.fromFunction(new UserServiceLive(_))
}


package com.valleyprogramming.myapp.api {

    import zio.*
    import zio.http.*
    import com.valleyprogramming.myapp.model.User
    import com.valleyprogramming.myapp.service.UserService

    /**
     * This code has a "service" parameter because it needs to access a
     * database/datastore.
     */
    object UserRoutes:
        def routes(userService: UserService): Routes[Any, Nothing] = {
            // EXERCISE: break this out into a variable/value
            Routes {
                Method.GET / "users" -> handler { (request: Request) =>
                    userService.getAllUsers()
                        // i only need `foldZIO` here because i want to add logging.
                        // if you don’t want logging, remove all the `ZIO.succeed` calls
                        // and the `ZIO.logInfo` call, and then change `foldZIO` to `fold`:
                        .foldZIO(
                            error => {
                                ZIO.logError("***** ERROR GETTING USERS FROM DB *****") *>
                                    ZIO.succeed(Response.status(Status.InternalServerError))
                            },
                            {
                                case Nil   =>
                                    ZIO.succeed(Response.status(Status.NoContent))
                                case users =>
                                    ZIO.succeed(Response.text(
                                        users.map(u => s"${u.id}: ${u.name}").mkString("\n")
                                    ))
                            }
                        )
                }
            }
        }

}  // end api



package com.valleyprogramming.myapp {

    import zio.*
    import zio.http.*
    import com.valleyprogramming.myapp.model.User
    import com.valleyprogramming.myapp.service.UserService
    import com.valleyprogramming.myapp.api.UserRoutes
    import com.valleyprogramming.myapp.config.DatabaseConfig
    import com.valleyprogramming.myapp.repository.UserRepository

    object ZioHttpMysqlApp extends ZIOAppDefault:

        val serverProgram = for
            _           <- ZIO.logInfo("***** APPLICATION STARTED *****")
            _           <- DatabaseConfig.init()
            userService <- ZIO.service[UserService]
            _           <- Server.serve(UserRoutes.routes(userService))
        yield
            ()

        override def run = serverProgram.provide(
            Server.default,
            UserRepository.layer,
            UserService.layer
        )

}


