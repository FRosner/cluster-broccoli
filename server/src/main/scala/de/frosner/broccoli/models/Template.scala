package de.frosner.broccoli.models

import de.frosner.broccoli.models.ParameterInfo.parameterInfoWrites
import org.apache.commons.codec.digest.DigestUtils
import play.api.libs.functional.syntax._
import play.api.libs.json._

import scala.collection.JavaConversions._

case class Template(id: String, template: String, description: String, parameterInfos: Map[String, ParameterInfo])
    extends Serializable {

  @transient
  lazy val parameters: Set[String] = parameterInfos.keySet

  // used for JSON serialization to have a deterministic order in the array representation of the set
  @transient
  lazy val sortedParameters: Seq[String] = parameters.toSeq.sorted

  // We sort the parameterInfos by the key to make the String deterministic
  @transient
  lazy val version: String = DigestUtils.md5Hex(template.trim() + "_" + parameterInfos.toSeq.sortBy(_._1).toString)

}

object Template {

  implicit val templateApiWrites: Writes[Template] = (
    (JsPath \ "id").write[String] and
      (JsPath \ "description").write[String] and
      (JsPath \ "parameters").write[Seq[String]] and
      (JsPath \ "parameterInfos").write[Map[String, ParameterInfo]] and
      (JsPath \ "version").write[String]
  )((template: Template) =>
    (template.id, template.description, template.sortedParameters, template.parameterInfos, template.version))

  implicit val templatePersistenceReads: Reads[Template] = Json.reads[Template]

  implicit val templatePersistenceWrites: Writes[Template] = Json.writes[Template]

}
