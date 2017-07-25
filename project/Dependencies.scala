import sbt._

object Dependencies {
  object Versions {
    // Note: Do not update to 1.13.x; specs2-scalacheck 3.6.6 requires Scalacheck 2.12.5 and does not work with 1.13.x
    val scalacheck = "1.12.5"

    // Note: Do not update; play2-specs requires specs2 3.6.6 and throws exceptions with newer versions
    val specs2 = "3.6.6"

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
    * Property testing for Scala.
    */
  val scalacheck: ModuleID = "org.scalacheck" %% "scalacheck" % Versions.scalacheck

  /**
    * Specs2 test framework, with support for property testing.
    */
  val specs2: Seq[ModuleID] = Seq("core", "scalacheck")
    .map(module => "org.specs2" %% s"specs2-$module" % Versions.specs2)

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
}
