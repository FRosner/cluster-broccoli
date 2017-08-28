package de.frosner.broccoli.templates

import com.typesafe.config.Config

/**
  * The Templates configuration.
  *
  * @param path The filesystem path to read the templates from
  */
final case class TemplateConfiguration(path: String)
