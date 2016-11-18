name := "Cluster Broccoli"

version := "0.5.0"

lazy val root = project.in(file(".")).enablePlugins(PlayScala, BuildInfoPlugin).
  settings(
    buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion),
    buildInfoPackage := "de.frosner.broccoli.build"
  )

libraryDependencies += ws

libraryDependencies += cache

libraryDependencies += specs2 % Test

libraryDependencies += "jp.t2v" %% "play2-auth" % "0.14.2"

libraryDependencies += "jp.t2v" %% "play2-auth-test"   % "0.14.2" % "test"

libraryDependencies += play.sbt.Play.autoImport.cache

resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"

routesGenerator := InjectedRoutesGenerator

parallelExecution in Test := false
