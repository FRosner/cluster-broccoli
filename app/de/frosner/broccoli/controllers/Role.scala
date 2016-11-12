package de.frosner.broccoli.controllers

sealed trait Role
case object Administrator extends Role
case object Operator extends Role
case object NormalUser extends Role