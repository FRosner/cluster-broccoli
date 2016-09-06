name := "Cluster Broccoli"

version := "0.4.0-SNAPSHOT"

lazy val root = project.in(file(".")).enablePlugins(PlayScala, BuildInfoPlugin).
  settings(
    buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion),
    buildInfoPackage := "de.frosner.broccoli.build"
  )

libraryDependencies += ws

libraryDependencies += specs2 % Test

resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"

routesGenerator := InjectedRoutesGenerator

