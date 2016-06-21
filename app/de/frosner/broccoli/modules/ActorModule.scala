package de.frosner.broccoli.modules

import com.google.inject.AbstractModule
import de.frosner.broccoli.services.{ConsulService, InstanceService, NomadService}
import play.api.libs.concurrent.AkkaGuiceSupport

class ActorModule extends AbstractModule with AkkaGuiceSupport {

  def configure() = {
    bindActor[NomadService]("nomad-actor")
    bindActor[ConsulService]("consul-actor")
    bindActor[InstanceService]("instance-actor")
  }

}
