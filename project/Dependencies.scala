import sbt._

object Dependencies {
  object Versions {
    // Note: Do not update to 1.13.x; specs2-scalacheck 3.6.6 requires Scalacheck 2.12.5 and does not work with 1.13.x
    val scalacheck = "1.12.5"

    // Note: Do not update; play2-specs requires specs2 3.6.6 and throws exceptions with newer versions
    val specs2 = "3.6.6"

    val play2auth = "0.14.2"

    val cats = "1.0.0-MF"

    val shapeless = "2.3.2"

    val scalaUri = "0.4.17"

    val squants = "1.3.0"

    val silhouette = "4.0.0"

    val pureconfig = "0.7.2"
  }

  /**
    * Typeclass boilerplate for Scala
    */
  val simulacrum: ModuleID = "com.github.mpilquist" %% "simulacrum" % "0.10.0"

  /**
    * Functional programming data types and type classes.
    */
  val cats: Seq[ModuleID] = Seq(
    "macros",
    "kernel",
    "core"
  ).map(module => "org.typelevel" %% s"cats-$module" % Versions.cats)

  /**
    * Type-level programming library
    */
  val shapeless: ModuleID = "com.chuusai" %% "shapeless" % Versions.shapeless

  /**
    * A powerful enumeration type for Scala.
    */
  val enumeratum: Seq[ModuleID] = Seq(
    "com.beachape" %% "enumeratum" % "1.5.12",
    // Keep this version at 1.5.11 for compatibility with Play 2.5; later versions depend on Play 2.6 already
    "com.beachape" %% "enumeratum-play-json" % "1.5.11"
  )

  /**
    * Types and DSL for URIs
    */
  val scalaUri: ModuleID = "io.lemonlabs" %% "scala-uri" % Versions.scalaUri

  /**
    * Units for Scala.
    *
    * We use it for size units, eg bytes, kilobytes, etc.
    */
  val squants: ModuleID = "org.typelevel" %% "squants" % Versions.squants

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
    * Authentication framework for Play.
    */
  val silhouette: Seq[ModuleID] = Seq(
    "com.mohiva" %% "play-silhouette" % Versions.silhouette,
    "com.mohiva" %% "play-silhouette-crypto-jca" % Versions.silhouette,
    "com.mohiva" %% "play-silhouette-password-bcrypt" % Versions.silhouette,
    "com.mohiva" %% "play-silhouette-persistence" % Versions.silhouette,
    "com.mohiva" %% "play-silhouette-testkit" % Versions.silhouette % Test
  )

  /**
    * Load configuration into case classes.
    */
  val pureconfig: Seq[ModuleID] = Seq(
    "pureconfig",
    "pureconfig-enumeratum"
  ).map(module => "com.github.pureconfig" %% module % Versions.pureconfig)

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

  /**
    * Apache Commons Lang to provide escape string functionality used during rendering parameter values in templates
    */
  val commonsLang: ModuleID = "org.apache.commons" % "commons-lang3" % "3.6"

  /**
    * Jinja templating engine implemented in Java
    */
  val jinJava: ModuleID = "com.hubspot.jinjava" % "jinjava" % "2.2.9"

}
