package de.frosner.broccoli.controllers

import java.io.File
import javax.inject.Inject

import de.frosner.broccoli.models.Template
import Template.{templateReads, templateWrites}
import play.{Logger, Play, Application}

import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import play.api.mvc.{Action, Controller}

import scala.io.Source
import scala.util.Try

//class TemplateController @Inject() (ws: WSClient) extends Controller {
class TemplateController @Inject() (configuration: play.api.Configuration) extends Controller {

  private val templatesDirectoryPath = configuration.getString("broccoli.templatesDir").getOrElse("templates")

  private val templates = {
    val templatesDirectory = new File(templatesDirectoryPath)
    Logger.info(s"Looking for templates in $templatesDirectoryPath")
    val templateDirectories = if (templatesDirectory.exists && templatesDirectory.isDirectory) {
      templatesDirectory.listFiles.filter(_.isDirectory).toSeq
    } else {
      Logger.error(s"Templates directory $templatesDirectoryPath is not a directory")
      Seq.empty
    }
    Logger.info(s"Found ${templateDirectories.size} template directories: ${templateDirectories.mkString(", ")}")

    val templates = templateDirectories.flatMap(templateDirectory => {
      val templateFile = new File(templateDirectory, "template.nomad")
      val tryFileContent = Try(Source.fromFile(templateFile).getLines.mkString("\n"))
      val tryTemplate = tryFileContent.map(content => Template(templateDirectory.getName, content))
      tryTemplate.failed.map(throwable => Logger.warn(s"Parsing $templateFile failed: $throwable"))
      tryTemplate.toOption
    })
    Logger.info(s"Successfully parsed ${templates.size} templates: ${templates.map(_.id).mkString(", ")}")
    templates
  }

  def list = Action {
    Ok(Json.toJson(templates.map(_.id)))
  }

  def show(id: String) = Action {
    templates.find(_.id == id).map(template => Ok(Json.toJson(template))).getOrElse(NotFound)
  }

  // TODO start implementation: https://www.playframework.com/documentation/2.5.x/ScalaWS

}
