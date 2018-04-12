package de.frosner.broccoli.services

case class ParameterValueParsingException(parameter: String) extends Exception(s"Could not parse parameter $parameter.")
