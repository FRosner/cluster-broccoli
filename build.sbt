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
      Dependencies.scalaguice
    ),
    libraryDependencies ++= Dependencies.play2auth,
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
        // Build settings
        scalacOptions ++= List(
          // Code encoding
          "-encoding",
          "UTF-8",
          // Deprecation warnings
          "-deprecation",
          // Warnings about features that should be imported explicitly
          "-feature",
          // Enable additional warnings about assumptions in the generated code
          "-unchecked",
          // Recommended additional warnings
          "-Xlint",
          // Warn when argument list is modified to match receiver
          "-Ywarn-adapted-args",
          // Warn about dead code
          "-Ywarn-dead-code",
          // Warn about inaccessible types in signatures
          "-Ywarn-inaccessible",
          // Warn when non-nullary overrides a nullary (def foo() over def foo)
          "-Ywarn-nullary-override",
          // Warn when numerics are unintentionally widened
          "-Ywarn-numeric-widen"
        ),
        // Play doesn't like parallel tests with all its state
        parallelExecution in Test := false
      ))
  )
  .aggregate(server)
