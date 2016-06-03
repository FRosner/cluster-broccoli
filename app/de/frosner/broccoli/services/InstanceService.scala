package de.frosner.broccoli.services

import javax.inject.Inject

import de.frosner.broccoli.models.Instance
import play.api.Configuration
import play.api.libs.ws.WSClient

class InstanceService @Inject() (configuration: Configuration, ws: WSClient) {

  val instances = Seq.empty[Instance]

}
