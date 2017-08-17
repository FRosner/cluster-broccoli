package de.frosner.broccoli.templates

import java.nio.file.{FileSystems, Files}

import de.frosner.broccoli.models.{Meta, ParameterInfo, Template}
import play.api.libs.json.Json

import scala.collection.JavaConverters._
import scala.io.Source
import scala.util.Try

/**
  * The template source that loads templates from a directory
  *
  * @param directory The path to the directory with templates
  */
class DirectoryTemplateSource(directory: String) extends TemplateSource {

  private val log = play.api.Logger(getClass)

  /**
    * @return The sequence of templates found in the directory
    */
  def loadTemplates(): Seq[Template] = {
    val rootTemplatesDirectory = FileSystems.getDefault.getPath(directory).toAbsolutePath

    if (!Files.isDirectory(rootTemplatesDirectory)) {
      throw new IllegalStateException(s"Templates directory ${rootTemplatesDirectory} is not a directory")
    }

    log.info(s"Looking for templates in $rootTemplatesDirectory")

    val templateDirectories = Files.list(rootTemplatesDirectory).iterator().asScala.filter(Files.isDirectory(_)).toSeq

    log.info(s"Found ${templateDirectories.length} template directories: ${templateDirectories.mkString(", ")}")

    val templates = templateDirectories.flatMap(templateDirectory => {
      val tryTemplate = Try {
        val templateFileContent = Source.fromFile(templateDirectory.resolve("template.json").toString).mkString
        val templateId = templateDirectory.getFileName.toString
        val metaFile = templateDirectory.resolve("meta.json")
        val metaFileContent = Try(Source.fromFile(metaFile.toString).mkString)
        val metaInformation = metaFileContent.map(content => Json.fromJson[Meta](Json.parse(content)).get)
        val defaultDescription = s"$templateId template"
        val description = metaInformation
          .map(_.description.getOrElse {
            log.warn(s"No description for $metaFile. Using default template description.")
            defaultDescription
          })
          .recover {
            case throwable =>
              log.warn(
                s"Failed to get description of $metaFile: ${throwable.getMessage}. Using default template description.",
                throwable)
              defaultDescription
          }
          .get

        val parameterInfos: Map[String, ParameterInfo] =
          metaInformation
            .map(meta =>
              meta.parameters
                .getOrElse {
                  log.warn(s"No parameters for $metaFile. Using default parameters.")
                  Map.empty[String, Meta.Parameter]
                }
                .map {
                  case (id, parameter) => id -> ParameterInfo.fromMetaParameter(id, parameter)
              })
            .recover {
              case throwable =>
                log.warn(s"Failed to get parameters of $metaFile: ${throwable.getMessage}. Using default parameters.",
                         throwable)
                Map.empty[String, ParameterInfo]
            }
            .get
        Template(templateId, templateFileContent, description, parameterInfos)
      }
      tryTemplate.failed.map(throwable => log.error(s"Parsing template '$templateDirectory' failed: $throwable"))
      tryTemplate.toOption
    })
    log.info(s"Successfully parsed ${templates.length} templates: ${templates.map(_.id).mkString(", ")}")
    templates.sortBy(_.id)
  }
}
