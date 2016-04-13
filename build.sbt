organization  := "de.frosner"

version       := "0.1.0-SNAPSHOT"

name          := "cluster-broccoli"

scalaVersion  := "2.11.7"

libraryDependencies += "org.scalatest" %% "scalatest" % "2.2.6" % "test"

libraryDependencies += "com.typesafe.akka" %% "akka-http-core" % "2.4.3"

libraryDependencies += "com.typesafe.akka" %% "akka-http-experimental" % "2.4.3"

fork := true
