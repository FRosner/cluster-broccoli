package de.frosner.broccoli.modules

import com.google.inject.AbstractModule
import de.frosner.broccoli.nomad.NomadActor
import play.api.libs.concurrent.AkkaGuiceSupport

class ActorModule extends AbstractModule with AkkaGuiceSupport {

  def configure() = {
    bindActor[NomadActor]("nomad-actor")
  }

}
