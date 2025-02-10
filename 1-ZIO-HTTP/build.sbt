ThisBuild / scalaVersion := "3.3.1"

// you REALLY want this when working with ZIO-HTTP.
// helps with killing the running app, and not having
// to exit the `sbt` shell:
run / fork := true

lazy val root = (project in file("."))
    .settings(
        name := "zio-http-examples",
        libraryDependencies ++= Seq(
            "dev.zio"    %% "zio"          % "2.1.6",
            "dev.zio"    %% "zio-http"     % "3.0.1",
            "dev.zio"    %% "zio-json"     % "0.6.2",
            "dev.zio"    %% "zio-logging"  % "2.3.2",
            "dev.zio"    %% "zio-logging-slf4j"  % "2.3.2",
            "org.slf4j"  %  "slf4j-api"    % "2.0.16",
            "org.slf4j"  %  "slf4j-simple" % "2.0.16",
        )
    )


// Compile / mainClass := Some("http_110.BasicRestApiWithSeparateRouteDefns")
// Compile / mainClass := Some("http_110b.BasicRestApiWithService")
// Compile / mainClass := Some("http_111a.BasicRestApiWithLogging")

// Compile / mainClass := Some("http_112.BasicAuthenticatedApi")
// Compile / mainClass := Some("http_112a.AuthHelperFunction")

Compile / mainClass := Some("http_112c.BasicAuthMiddleware")


// Compile / mainClass := Some("http_120.WebSocketEcho")


// Compile / mainClass := Some("alvin_100.FirstServer")
// Compile / mainClass := Some("alvin_103.PathParams")
// Compile / mainClass := Some("alvin_104.PathParamsAndJson")
// Compile / mainClass := Some("http_106.PathParamsAndJson")
// Compile / mainClass := Some("http_116.SignCookies")


