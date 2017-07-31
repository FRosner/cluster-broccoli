import sbt._
import sbt.Keys._
import sbt.plugins.JvmPlugin
import com.typesafe.sbt.packager.MappingsHelper.contentOf

/**
  * SBT plugin to build the frontend through yarn.
  *
  */
object YarnPlugin extends AutoPlugin {
  override val requires: Plugins = JvmPlugin
  override val trigger: PluginTrigger = NoTrigger

  object Commands {
    val install = Seq("yarn", "install")
    val setup = Seq("yarn", "setup")
    val dist = Seq("yarn", "dist", "--")
  }

  object autoImport {

    /**
      * Install Javascript packages with yarn.
      */
    val yarnInstall: TaskKey[Unit] = taskKey[Unit](s"execute: ${Commands.install}")

    /**
      * Setup the project.
      *
      * In our case, install Elm packages.
      */
    val yarnSetup: TaskKey[Unit] = taskKey[Unit](s"execute: ${Commands.setup}")

    /**
      * Build the webpack bundles through yarn dist.
      */
    val yarnDist: TaskKey[Seq[File]] = taskKey[Seq[File]](s"execute: ${Commands.dist}")
  }

  import autoImport._

  override def projectSettings: Seq[_root_.sbt.Def.Setting[_]] = Seq(
    cleanFiles ++= Seq(
      baseDirectory.value / "dist", // The webpack output
      baseDirectory.value / "node_modules", // The node modules
      baseDirectory.value / "elm-stuff" // Elm packages
    ),
    yarnSetup := {
      val base = baseDirectory.value
      val setup = FileFunction.cached(
        streams.value.cacheDirectory / "yarn-setup",
        inStyle = FilesInfo.hash,
        outStyle = FilesInfo.exists
      ) { _: Set[File] =>
        execute(Commands.setup, base, streams.value.log)
        (base / "elm-stuff" / "packages").get.toSet
      }
      setup((base / "elm-package.json").get.toSet)
    },
    yarnInstall := {
      val base = baseDirectory.value
      val install = FileFunction.cached(
        streams.value.cacheDirectory / "yarn-install",
        inStyle = FilesInfo.hash,
        outStyle = FilesInfo.exists
      ) { _: Set[File] =>
        execute(Commands.install, base, streams.value.log)
        (base / "node_modules").get.toSet
      }
      install((base * ("package.json" || "yarn.lock")).get.toSet)
    },
    yarnDist := {
      val log = streams.value.log
      log.info("Running webpack resource generator")
      val managedResources = (resourceManaged in Compile).value

      val yarnDist: Set[File] => Set[File] = FileFunction.cached(
        streams.value.cacheDirectory / "yarn-dist",
        inStyle = FilesInfo.hash,
        outStyle = FilesInfo.exists
      ) {
        _: Set[File] =>
          // Make sure that no assets from the previous run remain here
          val targetDirectory = managedResources / "public"
          IO.delete(targetDirectory)
          // Make webpack output to the managed resource directory.
          execute(Commands.dist ++ Seq("--output-path", targetDirectory.absolutePath),
                  baseDirectory.value,
                  streams.value.log)
          val generatedFiles = targetDirectory.***.get
          // show the generated files to the user to ease debugging
          generatedFiles.foreach(t => log.info(s"webpack generated $t"))
          generatedFiles.toSet
      }

      // Track all source files that affect the build result
      val sources = sourceDirectory.value.*** +++
        baseDirectory.value * ("package.json" || "yarn.lock" || "elm-package.json" || "webpack.config.*.js")
      yarnDist(sources.get.toSet).toSeq
    },
    yarnSetup := (yarnSetup dependsOn yarnInstall).value,
    yarnDist := (yarnDist dependsOn yarnSetup).value,
    resourceGenerators in Compile += yarnDist.taskValue
  )

  private def execute(cmd: Seq[String], workingDirectory: File, log: Logger): Unit = {
    val desc = s"executing: ${workingDirectory.toString}> ${cmd.mkString(" ")}"
    log.info(desc)
    val exitValue = Process(cmd, workingDirectory) ! log
    assert(exitValue == 0, s"Nonzero exit value '$exitValue', while $desc")
  }
}
