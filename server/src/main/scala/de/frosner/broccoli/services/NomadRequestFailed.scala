package de.frosner.broccoli.services

case class NomadRequestFailed(url: String, status: Int)
    extends Exception(s"Nomad request $url failed. Status code $status")
