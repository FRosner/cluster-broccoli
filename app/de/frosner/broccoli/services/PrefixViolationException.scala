package de.frosner.broccoli.services

case class PrefixViolationException(id: String, prefix: String)
    extends Throwable(s"ID '$id' did not have the required prefix '$prefix'")
