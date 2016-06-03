name := "cluster-broccoli"

version := "1.0.0"

lazy val root = project.in(file(".")).enablePlugins(PlayScala)

libraryDependencies += ws

libraryDependencies += specs2 % Test

routesGenerator := InjectedRoutesGenerator
