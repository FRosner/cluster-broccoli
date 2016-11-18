package de.frosner.broccoli.models

import de.frosner.broccoli.models.Role.Role

sealed trait Credentials {

  val name: String

  val password: String

}

case class UserCredentials(name: String, password: String) extends Credentials

sealed trait Account extends Credentials {

  val name: String

  val password: String

  val instanceRegex: String

  val role: Role

}

case class UserAccount(name: String,
                       password: String,
                       instanceRegex: String,
                       role: Role) extends Account

object Anonymous extends Account {

  val name = "anonymous"

  val password = ""

  val instanceRegex = ".*"

  val role = Role.Administrator

}

