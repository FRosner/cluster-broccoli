package de.frosner.broccoli.signal

import com.google.inject.AbstractModule
import net.codingwell.scalaguice.ScalaModule

class SignalModule extends AbstractModule with ScalaModule {
  override def configure(): Unit =
    bind[SignalManager].to[UnixSignalManager].asEagerSingleton()
}
