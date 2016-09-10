package de.frosner.broccoli.models

import play.api.libs.json.Json

import scala.util.parsing.combinator.JavaTokenParsers

case class Parameter(name: String, start: Int, end: Int)

object Parameter {

  implicit val parameterWrites = Json.writes[Parameter]

}

object ParameterParser extends JavaTokenParsers {
  def parameter(start: Int, end: Int): Parser[Parameter] = name ^^ { case name => Parameter(name, start, end)}
  def name: Parser[String] = "name" ~ ":" ~> Template.NameSyntax.r
  def apply(unparsed: String, start: Int, end: Int) = parseAll(parameter(start, end), unparsed)
}
