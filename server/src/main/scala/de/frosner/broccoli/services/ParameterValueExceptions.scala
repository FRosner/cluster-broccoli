package de.frosner.broccoli.services

case class ParameterValueParsingException(parameter: String) extends Exception(s"Could not parse parameter $parameter.")

case class ParameterNotFoundException(parameter: String, availParams: Set[String])
    extends Exception(s"Parameter '$parameter' not found. Available parameters are ${availParams.toString}")
