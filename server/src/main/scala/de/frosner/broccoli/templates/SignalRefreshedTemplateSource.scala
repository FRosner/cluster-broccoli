package de.frosner.broccoli.templates

import de.frosner.broccoli.models.Template
import de.frosner.broccoli.signal.UnixSignalManager
import sun.misc.{Signal, SignalHandler}

/**
  * The template source that wraps CachedTemplateSource and refreshes the cache after receiving SIGUSR2
  *
  * @param source The CachedTemplateSource that will be wrapped
  */
class SignalRefreshedTemplateSource(source: CachedTemplateSource, signalManager: UnixSignalManager)
    extends TemplateSource {
  signalManager.register(new Signal("USR2"), new SignalHandler() {
    def handle(sig: Signal) {
      source.refresh()
    }
  })

  override def loadTemplates(): Seq[Template] = source.loadTemplates()
}
