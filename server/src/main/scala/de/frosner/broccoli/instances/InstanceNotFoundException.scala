package de.frosner.broccoli.instances

case class InstanceNotFoundException(id: String) extends Exception(s"Instance '$id' not found")
