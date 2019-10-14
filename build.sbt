import com.typesafe.sbt.packager.docker._

lazy val webui = project
  .in(file("webui"))
  .enablePlugins(YarnPlugin)
  .settings(
    name := "Cluster Broccoli Web UI"
  )

lazy val server = project
  .in(file("server"))
  .enablePlugins(PlayScala, BuildInfoPlugin, DockerPlugin)
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
      Dependencies.commonsLang,
      Dependencies.jinJava,
      Dependencies.ficus,
      ws,
      guice,
      ehcache,
      specs2 % Test,
      specs2 % IntegrationTest,
      Dependencies.scalacheck % Test,
      Dependencies.commonsIO % Test
    ),
    libraryDependencies ++= Dependencies.cats,
    libraryDependencies ++= Dependencies.cats.map(_ % IntegrationTest),
    libraryDependencies ++= Dependencies.enumeratum,
    libraryDependencies ++= Dependencies.specs2.map(_ % Test),
    libraryDependencies ++= Dependencies.specs2.map(_ % IntegrationTest),
    libraryDependencies ++= Dependencies.silhouette,
    libraryDependencies ++= Dependencies.pureconfig,
    libraryDependencies ++= Dependencies.play2Iteratees,
    // Macro support for Scala
    addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full),
    buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion),
    buildInfoPackage := "de.frosner.broccoli.build",
    PlayKeys.playMonitoredFiles ++= (sourceDirectories in (Compile, TwirlKeys.compileTemplates)).value,
    dockerUsername := Some("datascienceplatform"),
    // Build from OpenJDK
    dockerBaseImage := "openjdk:8-jre",
    // Docker build settings
    packageName in Docker := "cluster-broccoli",
    maintainer in Docker := "Data Science Platform",
    // On Travis CI, use the commit hash as primary version of the docker image
    dockerAlias := dockerAlias.value.copy(tag = None),
    version in Docker := Option(System
      .getenv("TRAVIS_COMMIT"))
      .map(_.substring(0, 8))
      .getOrElse((version in Compile).value),
    // tag with the version but also the branch name (replacing '/' with '_')
    dockerBuildOptions := List(
      List("--force-rm",
           "-t",
           s"${dockerUsername.value.get}/${(packageName in Docker).value}:${(version in Docker).value}"),
      Option(System.getenv("TRAVIS_BRANCH"))
        .map(_.replaceAllLiterally("/", "_"))
        .map(name =>
          if (name == "master") "latest"
          else {
            val specials = Set('_', '.', '-')
            val notAllowedBegin = Set('.', '-)
            name.zipWithIndex
              .map {
                case (c, i) =>
                  val out = if (c.isLetterOrDigit || specials.contains(c)) c else '_'
                  i match {
                    case 0 => if (notAllowedBegin.contains(out)) '_' else out
                    case _ => out
                  }
              }
              .mkString("")
        })
        .map { tag =>
          List("-t", s"${dockerUsername.value.get}/${(packageName in Docker).value}:$tag")
        }
        .getOrElse(List.empty)
    ).flatten,
    // Upgrade the latest tag when we're building from master on Travis CI
    dockerUpdateLatest := Option(System.getenv("TRAVIS_BRANCH")).exists(_ == "master"),
    // Copy templates into the docker file
    mappings in Docker := {
      val templatesDirectory = baseDirectory.value.getParentFile / "templates"
      val targetDirectory = s"${(defaultLinuxInstallLocation in Docker).value}/templates"
      val templates = templatesDirectory.***.get.collect {
        case templateFile if templateFile.isFile =>
          templateFile -> templateFile
            .relativeTo(templatesDirectory)
            .map(name => s"$targetDirectory/${name.getPath}")
            .get
      }
      (mappings in Docker).value ++ templates
    },
    // Create a directory to store instances
    dockerCommands := {
      dockerCommands.value :+ ExecCmd("RUN", "mkdir", s"${(defaultLinuxInstallLocation in Docker).value}/instances")
    },
    // Expose the application port
    dockerExposedPorts := Seq(9000),
    // Add additional labels to track docker images back to builds and branches
    dockerLabels ++= Option(System.getenv("TRAVIS_BRANCH")).map("branch" -> _).toMap,
    dockerLabels ++= Option(System.getenv("TRAVIS_BUILD_NUMBER")).map("travis-build-number" -> _).toMap,
    // Do not run integration tests in parallel, because these spawn docker containers and thus depend on global state
    parallelExecution in IntegrationTest := false,
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
        version := "0.9.1-SNAPSHOT",
        scalaVersion := "2.12.8",
        // Enable jcenter for silhouette's dependencies
        resolvers += Resolver.jcenterRepo,
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
          "-Ywarn-numeric-widen",
          // Do not warn for unused implicits
          "-Ywarn-unused:-implicits"
        )
      ))
  )
  .aggregate(webui, server)
