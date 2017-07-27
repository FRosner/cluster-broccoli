package de.frosner.broccoli.templates

import com.typesafe.config.Config
import de.frosner.broccoli.util.Logging

/**
  * The Templates configuration.
  *
  * @param templatesPath The filesystem path to read the templates from
  */
final case class TemplateConfiguration(templatesPath: String)

object TemplateConfiguration extends Logging {

  /**
    * Extract a template configuration from a typesafe config.
    *
    * @param config The config
    * @return The corresponding nomad configuration
    */
  def fromConfig(config: Config): TemplateConfiguration = {
    val path = config.getString("path")

    Logger.info(s"template path=$path")

    TemplateConfiguration(path)
  }

}
