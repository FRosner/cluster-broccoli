lazy val webui = project
  .in(file("webui"))
  .enablePlugins(YarnPlugin)
  .settings(
    name := "Cluster Broccoli Web UI"
  )

lazy val server = project
  .in(file("server"))
  .enablePlugins(PlayScala, BuildInfoPlugin)
  .disablePlugins(PlayLayoutPlugin)
  .configs(IntegrationTest)
  .settings(Defaults.itSettings)
  .settings(
    name := "Cluster Broccoli",
    routesGenerator := InjectedRoutesGenerator,
    libraryDependencies ++= List(
      Dependencies.simulacrum,
      Dependencies.shapeless,
      Dependencies.scalaUri,
      Dependencies.squants,
      Dependencies.scalaguice,
      Dependencies.commonsIO,
      ws,
      cache,
      specs2 % Test,
      specs2 % IntegrationTest,
      Dependencies.scalacheck % Test
    ),
    libraryDependencies ++= Dependencies.cats,
    libraryDependencies ++= Dependencies.cats.map(_ % IntegrationTest),
    libraryDependencies ++= Dependencies.enumeratum,
    libraryDependencies ++= Dependencies.specs2.map(_ % Test),
    libraryDependencies ++= Dependencies.specs2.map(_ % IntegrationTest),
    libraryDependencies ++= Dependencies.play2auth,
    // Macro support for Scala
    addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full),
    buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion),
    buildInfoPackage := "de.frosner.broccoli.build",
    PlayKeys.playMonitoredFiles ++= (sourceDirectories in (Compile, TwirlKeys.compileTemplates)).value,
    // Do not run integration tests in parallel, because these spawn docker containers and thus depend on global state
    parallelExecution in IntegrationTest := false,
    // Do not run unit tests in parallel either because Play doesn't like it
    // Play doesn't like parallel tests with all its state
    parallelExecution in Test := false,
    // Disable API documentation, see https://github.com/playframework/playframework/issues/6688#issuecomment-258080633.
    // Scaladoc shouldn't break our build, and is known to trigger random errors for some code, see eg,
    // https://stackoverflow.com/q/19315362/355252.
    //
    // FIXME: With Play 2.6, replace with the new includeDocumentation setting introduced in
    // https://github.com/playframework/playframework/pull/6723
    sources in (Compile, doc) := Seq.empty
  )
  .dependsOn(webui)

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
        )
      ))
  )
  .aggregate(webui, server)
