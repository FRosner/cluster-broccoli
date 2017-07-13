lazy val root = project
  .in(file("."))
  .enablePlugins(PlayScala, BuildInfoPlugin)
  .settings(
    name := "Cluster Broccoli",
    routesGenerator := InjectedRoutesGenerator,
    libraryDependencies ++= List(
      ws,
      cache,
      specs2 % Test,
      // Play authentication framework
      "jp.t2v" %% "play2-auth" % "0.14.2",
      "jp.t2v" %% "play2-auth-test" % "0.14.2" % "test",
      // Functional programming tools
      "org.typelevel" %% "cats-core" % "0.9.0"
    ),
    buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion),
    buildInfoPackage := "de.frosner.broccoli.build"
  )
  .settings(
    inThisBuild(
      Seq(
        version := "0.7.0-SNAPSHOT",
        scalaVersion := "2.11.11",
        resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases",
        parallelExecution in Test := false
      ))
  )
