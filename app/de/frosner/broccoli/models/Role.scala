package de.frosner.broccoli.models

sealed trait Role
case object Administrator extends Role
case object Operator extends Role
case object NormalUser extends Role