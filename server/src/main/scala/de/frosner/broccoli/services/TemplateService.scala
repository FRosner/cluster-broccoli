package de.frosner.broccoli.services

import javax.inject.Inject
import javax.inject.Singleton

import de.frosner.broccoli.conf
import de.frosner.broccoli.models.Template
import de.frosner.broccoli.util.Logging
import de.frosner.broccoli.templates._
import play.api.Configuration

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

  def getTemplates: Seq[Template] = {
    if (templatesStorageType != conf.TEMPLATES_STORAGE_TYPE_FILESYSTEM) {
      throw new IllegalStateException(s"$templatesStorageType not supported")
    }

    new SignalRefreshedTemplateSource(new CachedTemplateSource(new DirectoryTemplateSource(templatesUrl)))
      .loadTemplates()
  }

  def template(id: String): Option[Template] = getTemplates.find(_.id == id)

}
