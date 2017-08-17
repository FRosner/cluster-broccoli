package de.frosner.broccoli.templates

import com.typesafe.config.Config

/**
  * The Templates configuration.
  *
  * @param templatesPath The filesystem path to read the templates from
  */
final case class TemplateConfiguration(templatesPath: String)

object TemplateConfiguration {

  /**
    * Extract a template configuration from a typesafe config.
    *
    * @param config The config
    * @return The corresponding nomad configuration
    */
  def fromConfig(config: Config): TemplateConfiguration =
    TemplateConfiguration(config.getString("path"))

}
