// Enable partial unification of types, for various scala versions
addSbtPlugin("org.lyranthe.sbt" % "partial-unification" % "1.1.0")

// Play framework
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.6.20")

// Build metadata available at runtime
addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.6.1")

// Docker packaging for Broccoli
addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.2.1")
