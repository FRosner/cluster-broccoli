package de.frosner.broccoli.services

import java.io.{File, FileNotFoundException}
import javax.inject.Inject
import javax.inject.Singleton

import de.frosner.broccoli.conf
import de.frosner.broccoli.models.{ParameterInfo, Template}
import de.frosner.broccoli.util.Logging
import play.api.Configuration
import play.libs.Json

import scala.collection.JavaConversions
import scala.io.Source
import scala.util.{Failure, Success, Try}

@Singleton
class TemplateService @Inject() (configuration: Configuration) extends Logging {

  private lazy val templatesStorageType = {
    val storageType = configuration.getString(conf.TEMPLATES_STORAGE_TYPE_KEY).getOrElse(conf.TEMPLATES_STORAGE_TYPE_DEFAULT)
    if (storageType != conf.TEMPLATES_STORAGE_TYPE_FILESYSTEM) {
      Logger.error(s"${conf.TEMPLATES_STORAGE_TYPE_KEY}=$storageType is invalid. Only '${conf.TEMPLATES_STORAGE_TYPE_FILESYSTEM}' supported.")
      System.exit(1)
    }
    Logger.info(s"${conf.TEMPLATES_STORAGE_TYPE_KEY}=$storageType")
    storageType
  }
  private lazy val templatesUrl = {
    if (configuration.getString("broccoli.templatesDir").isDefined) Logger.warn(s"'broccoli.templatesDir' ignored. Use ${conf.TEMPLATES_STORAGE_FS_URL_KEY} instead.")
    val url = configuration.getString(conf.TEMPLATES_STORAGE_FS_URL_KEY).getOrElse(conf.TEMPLATES_STORAGE_FS_URL_DEFAULT)
    Logger.info(s"${conf.TEMPLATES_STORAGE_FS_URL_KEY}=$url")
    url
  }

  private lazy val templates: Seq[Template] = {
    if (templatesStorageType == conf.TEMPLATES_STORAGE_TYPE_FILESYSTEM) {
      val templatesDirectory = new File(templatesUrl)
      Logger.info(s"Looking for templates in $templatesUrl")
      val templateDirectories = if (templatesDirectory.exists && templatesDirectory.isDirectory) {
        templatesDirectory.listFiles.filter(_.isDirectory).toSeq
      } else {
        Logger.error(s"Templates directory $templatesUrl is not a directory")
        Seq.empty
      }
      Logger.info(s"Found ${templateDirectories.size} template directories: ${templateDirectories.mkString(", ")}")

      val templates = templateDirectories.flatMap(templateDirectory => {
        val tryTemplate = Try {
          val templateFile = new File(templateDirectory, "template.json")
          val templateFileContent = Source.fromFile(templateFile).getLines.mkString("\n")
          val templateId = templateDirectory.getName
          val metaFile = new File(templateDirectory, "meta.json")
          val metaFileContent = Try(Source.fromFile(metaFile).getLines.mkString("\n"))
          val metaInformation = metaFileContent.map(content => Json.parse(content))
          val defaultDescription = s"$templateId template"
          val description = metaInformation.flatMap { information =>
            if (information.has("description"))
              Success(information.get("description").asText)
            else
              Failure(new IllegalArgumentException(s"No 'description' field specified in $metaFile. Using default template description."))
          }.recover {
            case fileNotFound: FileNotFoundException =>
              Logger.warn(s"No $metaFile file specified. Using default template description. $fileNotFound")
              defaultDescription
            case throwable =>
              Logger.warn(throwable.toString)
              defaultDescription
          }.get
          val defaultParameterInfos = Map.empty[String, ParameterInfo]
          val parameterInfos: Map[String, ParameterInfo] = metaInformation.flatMap { information =>
            if (information.has("parameters")) {
              val fields = JavaConversions.asScalaIterator(information.get("parameters").fields()).toIterable
              val parameterInfoMap = fields.map { entry =>
                val name = entry.getKey
                val entryValue = entry.getValue
                val default = if (entryValue.has("default")) Some(entryValue.get("default").asText) else None
                val secret = if (entryValue.has("secret")) Some(entryValue.get("secret").asBoolean()) else None
                (name, ParameterInfo(name, default, secret))
              }.toMap
              Success(parameterInfoMap)
            } else {
              Failure(new IllegalArgumentException(s"No 'parameters' field in $metaFile. Not parsing any additional parameter information."))
            }
          }.recover {
            case fileNotFound: FileNotFoundException =>
              Logger.warn(s"No 'meta.json' file specified. Not parsing any additional parameter information. $fileNotFound")
              defaultParameterInfos
            case throwable =>
              Logger.warn(throwable.toString)
              defaultParameterInfos
          }.get
          Template(templateId, templateFileContent, description, parameterInfos)
        }
        tryTemplate.failed.map(throwable => Logger.error(s"Parsing template '$templateDirectory' failed: $throwable"))
        tryTemplate.toOption
      })
      Logger.info(s"Successfully parsed ${templates.size} templates: ${templates.map(_.id).mkString(", ")}")
      templates.sortBy(_.id)
    } else {
      throw new IllegalStateException(s"$templatesStorageType not supported")
    }
  }

  def getTemplates: Seq[Template] = templates

  def template(id: String): Option[Template] = templates.find(_.id == id)

}
