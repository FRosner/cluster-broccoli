import sbt._

object Dependencies {
  object Versions {
    val play2auth = "0.14.2"
  }

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
