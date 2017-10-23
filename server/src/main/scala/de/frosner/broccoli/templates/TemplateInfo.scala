package de.frosner.broccoli.templates

import de.frosner.broccoli.models.ParameterInfo

case class TemplateInfo(description: Option[String], parameters: Option[Seq[ParameterInfo]])
