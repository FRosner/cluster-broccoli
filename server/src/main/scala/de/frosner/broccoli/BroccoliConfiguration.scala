package de.frosner.broccoli

import de.frosner.broccoli.instances.InstanceConfiguration
import de.frosner.broccoli.nomad.NomadConfiguration
import de.frosner.broccoli.templates.TemplateConfiguration
import de.frosner.broccoli.websocket.WebSocketConfiguration

/**
  * The broccoli configuration.
  */
final case class BroccoliConfiguration(
    nomad: NomadConfiguration,
    templates: TemplateConfiguration,
    instances: InstanceConfiguration,
    webSocket: WebSocketConfiguration
)
