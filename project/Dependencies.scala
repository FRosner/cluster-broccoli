import sbt._

object Dependencies {
  object Versions {
    val play2auth = "0.14.2"
  }

  /**
    * A powerful enumeration type for Scala.
    */
  val enumeratum: Seq[ModuleID] = Seq(
    "com.beachape" %% "enumeratum" % "1.5.12",
    // Keep this version at 1.5.11 for compatibility with Play 2.5; later versions depend on Play 2.6 already
    "com.beachape" %% "enumeratum-play-json" % "1.5.11"
  )

  /**
    * Scala syntax for Guice, to declare custom modules
    */
  val scalaguice: ModuleID = "net.codingwell" %% "scala-guice" % "4.1.0"

  /**
    * Authentication framework for Play
    */
  val play2auth: Seq[ModuleID] = Seq(
    "jp.t2v" %% "play2-auth" % Versions.play2auth,
    "jp.t2v" %% "play2-auth-test" % Versions.play2auth % Test,
    "jp.t2v" %% "play2-auth-test" % Versions.play2auth % IntegrationTest
  )

  /**
    * Apache Commons IO to provide additional functionality for IO like copying directories recursively
    */
  val commonsIO: ModuleID = "commons-io" % "commons-io" % "2.5"

}
