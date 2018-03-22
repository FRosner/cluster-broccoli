package de.frosner.broccoli.services

import de.frosner.broccoli.models.Template

case class TemplateParameterNotFoundException(templateId: String, parameter: String)
    extends Exception(s"Template '$templateId' did not have parameter $parameter")
