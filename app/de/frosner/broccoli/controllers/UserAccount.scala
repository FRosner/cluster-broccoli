package de.frosner.broccoli.controllers

sealed trait Account {

  val name: String

  val password: String

}

case class UserAccount(name: String, password: String) extends Account

object Anonymous extends Account {

  val name = "anonymous"

  val password = ""

}

