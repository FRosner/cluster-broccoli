package de.frosner.broccoli.models

import play.api.libs.json.Json

case class RefreshRequest(token: String, returnTemplates: Boolean)

object Refresh {
  implicit val refreshRequestReads = Json.reads[RefreshRequest]
}
