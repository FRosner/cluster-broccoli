package de.frosner.broccoli.models

sealed trait Credentials {

  val name: String

  val password: String

}

case class UserCredentials(name: String, password: String) extends Credentials

sealed trait Account extends Credentials {

  val name: String

  val password: String

  val instanceRegex: String

}

case class UserAccount(name: String,
                       password: String,
                       instanceRegex: String) extends Account

object Anonymous extends Account {

  val name = "anonymous"

  val password = ""

  val instanceRegex = ".*"

}

