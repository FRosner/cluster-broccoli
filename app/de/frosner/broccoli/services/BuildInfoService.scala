package de.frosner.broccoli.services

import java.io.File
import javax.inject.{Inject, Singleton}

import de.frosner.broccoli.conf
import de.frosner.broccoli.models.Template
import play.Logger
import play.api.Configuration

import scala.io.Source
import scala.util.Try

@Singleton
class BuildInfoService {

  val projectName = de.frosner.broccoli.build.BuildInfo.name
  val projectVersion = de.frosner.broccoli.build.BuildInfo.version
  val scalaVersion = de.frosner.broccoli.build.BuildInfo.scalaVersion
  val sbtVersion = de.frosner.broccoli.build.BuildInfo.sbtVersion

}
