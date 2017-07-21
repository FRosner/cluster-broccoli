import sbt._

object Dependencies {
  object Versions {
    val play2auth = "0.14.2"
  }

  /**
    * Play authentication framework.
    */
  val play2auth: Seq[ModuleID] = Seq(
    "jp.t2v" %% "play2-auth" % Versions.play2auth,
    "jp.t2v" %% "play2-auth-test" % Versions.play2auth % Test
  )
}
