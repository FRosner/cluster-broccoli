package de.frosner.broccoli.services

import de.frosner.broccoli.models.Template

case class ParameterValueParsingException(templateId: String, parameter: String)
    extends Exception(s"Could not parse parameter $parameter in template '${templateId}'.")
