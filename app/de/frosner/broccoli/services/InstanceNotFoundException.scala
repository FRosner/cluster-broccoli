package de.frosner.broccoli.services

case class InstanceNotFoundException(id: String) extends Exception(s"Instance '$id' not found")
