package de.frosner.broccoli.conf

class IllegalConfigException(property: String, reason: String)
  extends Exception(s"Illegal value of $property: $reason")
