package de.frosner.broccoli.services

case class TemplateNotFoundException(id: String) extends Exception(s"Template '$id' not found")
