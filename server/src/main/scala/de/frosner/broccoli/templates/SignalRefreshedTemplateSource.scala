package de.frosner.broccoli.templates

import javax.inject.Singleton

import de.frosner.broccoli.models.Template
import sun.misc.{Signal, SignalHandler}

@Singleton
class SignalRefreshedTemplateSource(source: CachedTemplateSource) extends TemplateSource {
  Signal.handle(new Signal("USR2"), new SignalHandler() {
    def handle(sig: Signal) {
      source.refresh()
    }
  })

  override def loadTemplates(): Seq[Template] = source.loadTemplates()
}
