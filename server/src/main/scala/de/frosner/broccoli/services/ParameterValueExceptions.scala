package de.frosner.broccoli.services

case class ParameterValueParsingException(parameter: String, reason: String)
    extends Exception(s"Could not parse parameter $parameter. $reason")

case class ParameterNotFoundException(parameter: String, availParams: Set[String])
    extends Exception(s"Parameter '$parameter' not found. Available parameters are ${availParams.toString}")

case class ParameterMetadataException(message: String)
  extends Exception(s"Error parsing parameter metadata. $message")
