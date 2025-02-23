ThisBuild / scalaVersion := "3.3.1"

// this helps with killing the running app, and not having
// to exit the `sbt` shell:
run / fork := true

lazy val root = (project in file("."))
    .settings(
        name := "zio-http-mysql",
        libraryDependencies ++= Seq(
            "dev.zio"         %% "zio-http" % "3.0.1",
            "org.scalikejdbc" %% "scalikejdbc" % "4.2.0",
            "mysql"           %  "mysql-connector-java" % "8.0.33",

            // i added this and 'src/main/resources/logback.xml' because
            // Netty was producing a lot of DEBUG output by default:
            "ch.qos.logback"  %  "logback-classic" % "1.4.11"
        )
    )

