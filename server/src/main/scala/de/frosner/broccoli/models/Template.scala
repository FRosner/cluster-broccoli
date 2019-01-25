package de.frosner.broccoli.models

import de.frosner.broccoli.auth.Account
import org.apache.commons.codec.digest.DigestUtils
import play.api.libs.functional.syntax._
import play.api.libs.json.Json.JsValueWrapper
import play.api.libs.json._

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

  implicit def paramInfoMapWrites(implicit account: Account): Writes[Map[String, ParameterInfo]] =
    new Writes[Map[String, ParameterInfo]] {
      def writes(map: Map[String, ParameterInfo]): JsValue =
        Json.obj(map.map {
          case (s, paramInfo) =>
            val ret: (String, JsValueWrapper) = s -> Json.toJson(paramInfo)(ParameterInfo.parameterInfoApiWrites)
            ret
        }.toSeq: _*)
    }

  implicit def templateApiWrites(implicit account: Account): Writes[Template] =
    (
      (JsPath \ "id").write[String] and
        (JsPath \ "description").write[String] and
        (JsPath \ "parameters").write[Seq[String]] and
        (JsPath \ "parameterInfos").write[Map[String, ParameterInfo]](paramInfoMapWrites) and
        (JsPath \ "version").write[String]
    )((template: Template) =>
      (template.id, template.description, template.sortedParameters, template.parameterInfos, template.version))

  implicit val templatePersistenceReads: Reads[Template] = Json.reads[Template]

  implicit val templatePersistenceWrites: Writes[Template] = Json.writes[Template]

}
