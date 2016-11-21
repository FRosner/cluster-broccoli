package de.frosner.broccoli.conf

case class IllegalConfigException(property: String, reason: String)
  extends Exception(s"Illegal value of $property: $reason")
