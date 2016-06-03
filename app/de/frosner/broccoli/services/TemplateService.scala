package de.frosner.broccoli.services

import java.io.File
import javax.inject.Inject

import de.frosner.broccoli.models.Template
import play.Logger

import scala.io.Source
import scala.util.Try

class TemplateService @Inject() (configuration: play.api.Configuration) {

  private val templatesDirectoryPath = configuration.getString("broccoli.templatesDir").getOrElse("templates")

  val templates = {
    val templatesDirectory = new File(templatesDirectoryPath)
    Logger.info(s"Looking for templates in $templatesDirectoryPath")
    val templateDirectories = if (templatesDirectory.exists && templatesDirectory.isDirectory) {
      templatesDirectory.listFiles.filter(_.isDirectory).toSeq
    } else {
      Logger.error(s"Templates directory $templatesDirectoryPath is not a directory")
      Seq.empty
    }
    Logger.info(s"Found ${templateDirectories.size} template directories: ${templateDirectories.mkString(", ")}")

    val templates = templateDirectories.flatMap(templateDirectory => {
      val templateFile = new File(templateDirectory, "template.nomad")
      val tryFileContent = Try(Source.fromFile(templateFile).getLines.mkString("\n"))
      val tryTemplate = tryFileContent.map(content => Template(templateDirectory.getName, content))
      tryTemplate.failed.map(throwable => Logger.warn(s"Parsing $templateFile failed: $throwable"))
      tryTemplate.toOption
    })
    Logger.info(s"Successfully parsed ${templates.size} templates: ${templates.map(_.id).mkString(", ")}")
    templates
  }

}
