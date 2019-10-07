package de.frosner.broccoli.templates

import de.frosner.broccoli.templates.jinjava.JinjavaConfiguration

/**
  * The Templates configuration.
  *
  * @param path The filesystem path to read the templates from
  * @param jinjava Configuration specific to jinjava template library
  */
final case class TemplateConfiguration(path: String, jinjava: JinjavaConfiguration, reloadToken: String)
