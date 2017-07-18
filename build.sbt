lazy val server = project
  .in(file("server"))
  .enablePlugins(PlayScala, BuildInfoPlugin)
  .disablePlugins(PlayLayoutPlugin)
  .settings(
    name := "Cluster Broccoli",
    routesGenerator := InjectedRoutesGenerator,
    libraryDependencies ++= List(
      ws,
      cache,
      specs2 % Test,
      "jp.t2v" %% "play2-auth" % "0.14.2",
      "jp.t2v" %% "play2-auth-test" % "0.14.2" % "test"
    ),
    buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion),
    buildInfoPackage := "de.frosner.broccoli.build",
    PlayKeys.playMonitoredFiles ++= (sourceDirectories in (Compile, TwirlKeys.compileTemplates)).value
  )

lazy val root = project
  .in(file("."))
  .settings(
    inThisBuild(
      List(
        version := "0.7.0-SNAPSHOT",
        scalaVersion := "2.11.11",
        resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases",
        parallelExecution in Test := false
      ))
  )
  .aggregate(server)
