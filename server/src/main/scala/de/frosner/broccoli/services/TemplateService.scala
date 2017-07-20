package de.frosner.broccoli.services

import java.nio.file.{FileSystems, Files}
import javax.inject.Inject
import javax.inject.Singleton

import de.frosner.broccoli.conf
import de.frosner.broccoli.models.{Meta, ParameterInfo, Template}
import de.frosner.broccoli.util.Logging
import play.api.Configuration
import play.api.libs.json.Json

import scala.collection.JavaConverters._
import scala.io.Source
import scala.util.Try

@Singleton
class TemplateService @Inject()(configuration: Configuration) extends Logging {

  private lazy val templatesStorageType = {
    val storageType =
      configuration.getString(conf.TEMPLATES_STORAGE_TYPE_KEY).getOrElse(conf.TEMPLATES_STORAGE_TYPE_DEFAULT)
    if (storageType != conf.TEMPLATES_STORAGE_TYPE_FILESYSTEM) {
      Logger.error(
        s"${conf.TEMPLATES_STORAGE_TYPE_KEY}=$storageType is invalid. Only '${conf.TEMPLATES_STORAGE_TYPE_FILESYSTEM}' supported.")
      System.exit(1)
    }
    Logger.info(s"${conf.TEMPLATES_STORAGE_TYPE_KEY}=$storageType")
    storageType
  }
  private lazy val templatesUrl = {
    if (configuration.getString("broccoli.templatesDir").isDefined)
      Logger.warn(s"'broccoli.templatesDir' ignored. Use ${conf.TEMPLATES_STORAGE_FS_URL_KEY} instead.")
    val url =
      configuration.getString(conf.TEMPLATES_STORAGE_FS_URL_KEY).getOrElse(conf.TEMPLATES_STORAGE_FS_URL_DEFAULT)
    Logger.info(s"${conf.TEMPLATES_STORAGE_FS_URL_KEY}=$url")
    url
  }

  private lazy val templates: Seq[Template] = {
    if (templatesStorageType != conf.TEMPLATES_STORAGE_TYPE_FILESYSTEM) {
      throw new IllegalStateException(s"$templatesStorageType not supported")
    }

    val rootTemplatesDirectory = FileSystems.getDefault.getPath(templatesUrl)

    if (!Files.isDirectory(rootTemplatesDirectory)) {
      throw new IllegalStateException(s"Templates directory $templatesUrl is not a directory")
    }

    Logger.info(s"Looking for templates in $templatesUrl")

    val templateDirectories = Files.list(rootTemplatesDirectory).iterator().asScala.filter(Files.isDirectory(_)).toSeq

    Logger.info(s"Found ${templateDirectories.length} template directories: ${templateDirectories.mkString(", ")}")

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
            Logger.warn(s"No description for $metaFile. Using default template description.")
            defaultDescription
          })
          .recover {
            case throwable =>
              Logger.warn(
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
                  Logger.warn(s"No parameters for $metaFile. Using default parameters.")
                  Map.empty[String, Meta.Parameter]
                }
                .map {
                  case (id, parameter) => id -> ParameterInfo.fromMetaParameter(id, parameter)
              })
            .recover {
              case throwable =>
                Logger.warn(
                  s"Failed to get parameters of $metaFile: ${throwable.getMessage}. Using default parameters.",
                  throwable)
                Map.empty[String, ParameterInfo]
            }
            .get
        Template(templateId, templateFileContent, description, parameterInfos)
      }
      tryTemplate.failed.map(throwable => Logger.error(s"Parsing template '$templateDirectory' failed: $throwable"))
      tryTemplate.toOption
    })
    Logger.info(s"Successfully parsed ${templates.length} templates: ${templates.map(_.id).mkString(", ")}")
    templates.sortBy(_.id)
  }

  def getTemplates: Seq[Template] = templates

  def template(id: String): Option[Template] = templates.find(_.id == id)

}
