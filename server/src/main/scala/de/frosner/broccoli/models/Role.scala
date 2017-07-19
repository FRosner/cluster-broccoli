package de.frosner.broccoli.models

import de.frosner.broccoli.models.Role.Role
import play.api.libs.json.{JsString, Reads, Writes}

object Role extends Enumeration {

  type Role = Value

  val Administrator = Value("administrator")
  val Operator = Value("operator")
  val NormalUser = Value("user")

}

object RoleJson {

  implicit val roleWrites: Writes[Role] = Writes(value => JsString(value.toString))

  implicit val roleReads: Reads[Role] = Reads(_.validate[String].map(Role.withName))

}