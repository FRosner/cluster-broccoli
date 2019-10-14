package de.frosner.broccoli.models

import de.frosner.broccoli.auth.Account
import org.apache.commons.codec.digest.DigestUtils
import play.api.libs.functional.syntax._
import play.api.libs.json.Json.JsValueWrapper
import play.api.libs.json._

case class Template(id: String,
                    template: String,
                    description: String,
                    parameterInfos: Map[String, ParameterInfo],
                    format: TemplateFormat = TemplateFormat.JSON)
    extends Serializable {

  @transient
  lazy val parameters: Set[String] = parameterInfos.keySet

  // used for JSON serialization to have a deterministic order in the array representation of the set
  @transient
  lazy val sortedParameters: Seq[String] = parameters.toSeq.sorted

  // We sort the parameterInfos by the key to make the String deterministic
  // We need to convert to ArrayBuffer because scala 2.10 used ArrayBuffer and we don't want the hash to change
  // TODO: Change method for calculating version as the current way is version dependent
  @transient
  lazy val version: String =
    DigestUtils.md5Hex(template.trim() + "_" + parameterInfos.toSeq.sortBy(_._1).toBuffer.toString)

}

object Template {

  // Supported from Play 2.6. Automatically populates default fields when reading json
  implicit def jsonFormat = Json.using[Json.WithDefaultValues].format[Template]

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
