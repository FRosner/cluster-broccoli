import sbt._

object Dependencies {
  object Versions {
    val scalacheck = "1.14.0"

    val specs2 = "4.4.1"

    val cats = "1.6.0"

    val shapeless = "2.3.2"

    val scalaUri = "1.4.0"

    val squants = "1.3.0"

    val silhouette = "5.0.7"

    val pureconfig = "0.7.2"

    val playIteratees = "2.6.1"

  }

  /**
    * Typeclass boilerplate for Scala
    */
  val simulacrum: ModuleID = "com.github.mpilquist" %% "simulacrum" % "0.12.0"

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
    "com.beachape" %% "enumeratum" % "1.5.13",
    // Keep this version at 1.5.11 for compatibility with Play 2.5; later versions depend on Play 2.6 already
    "com.beachape" %% "enumeratum-play-json" % "1.5.13"
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
  val specs2: Seq[ModuleID] = Seq("core", "matcher", "common", "scalacheck", "scalaz", "junit")
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
    * Iteratees/Enumerators were removed from the play library since play 2.6.x and moved to a separate
    * repository at: https://github.com/playframework/play-iteratees
    */
  val play2Iteratees: Seq[ModuleID] = Seq(
    "com.typesafe.play" %% "play-iteratees" % Versions.playIteratees,
    "com.typesafe.play" %% "play-iteratees-reactive-streams" % Versions.playIteratees
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
  val jinJava: ModuleID = "com.hubspot.jinjava" % "jinjava" % "2.5.2"

  /**
    * Ficus is a lightweight companion to Typesafe config that makes it more Scala-friendly.
    * Ficus adds an as[A] method to a normal Typesafe Config so you can do things like config.as[ Option[Int] ]
    */
  val ficus: ModuleID = "com.iheart" %% "ficus" % "1.4.3"

}
