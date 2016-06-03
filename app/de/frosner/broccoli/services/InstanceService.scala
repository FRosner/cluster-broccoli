package de.frosner.broccoli.services

import javax.inject.Inject

import de.frosner.broccoli.models.Instance

class InstanceService @Inject() (configuration: play.api.Configuration) {

  val instances = Seq.empty[Instance]

}
