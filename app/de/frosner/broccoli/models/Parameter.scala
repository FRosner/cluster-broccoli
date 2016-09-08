package de.frosner.broccoli.models

import scala.util.parsing.combinator.JavaTokenParsers

case class Parameter(name: String)

object ParameterParser extends JavaTokenParsers {
  def parameter: Parser[Parameter] = name ^^ { case name => Parameter(name)}
  def name: Parser[String] = "name" ~ ":" ~> Template.NameSyntax.r
  def apply(s: String) = parseAll(parameter, s)
}
