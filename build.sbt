name := "cluster-broccoli"

version := "0.2.0-SNAPSHOT"

lazy val root = project.in(file(".")).enablePlugins(PlayScala)

libraryDependencies += ws

libraryDependencies += specs2 % Test

libraryDependencies += "com.typesafe.akka" %% "akka-testkit" % "2.3.13"

resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"

routesGenerator := InjectedRoutesGenerator

