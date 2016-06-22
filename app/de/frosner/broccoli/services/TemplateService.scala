package de.frosner.broccoli.services

import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

import de.frosner.broccoli.conf
import de.frosner.broccoli.models.Template
import play.Logger
import play.api.Configuration

import scala.io.Source
import scala.util.Try

@Singleton
class TemplateService @Inject() (configuration: Configuration) {

  private val templatesDirectoryPath = configuration.getString(conf.TEMPLATES_DIR_KEY).getOrElse(conf.TEMPLATES_DIR_DEFAULT)

  val templates: Seq[Template] = {
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
      val tryTemplate = Try {
        val templateFile = new File(templateDirectory, "template.json")
        val templateFileContent = Source.fromFile(templateFile).getLines.mkString("\n")
        val descriptionFileContent = Try {
          Source.fromFile(new File(templateDirectory, "description.txt")).getLines.mkString("\n")
        }
        val templateId = templateDirectory.getName
        Template(templateId, templateFileContent, descriptionFileContent.getOrElse(s"Template $templateId."))
      }
      tryTemplate.failed.map(throwable => Logger.warn(s"Parsing template failed: $throwable"))
      tryTemplate.toOption
    })
    Logger.info(s"Successfully parsed ${templates.size} templates: ${templates.map(_.id).mkString(", ")}")
    templates
  }

  def template(id: String): Option[Template] = templates.find(_.id == id)

}
