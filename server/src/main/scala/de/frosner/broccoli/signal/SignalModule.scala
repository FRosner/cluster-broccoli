package de.frosner.broccoli.signal

import javax.inject.Singleton

import com.google.inject.{AbstractModule, Provides}
import net.codingwell.scalaguice.ScalaModule

class SignalModule extends AbstractModule with ScalaModule {
  override def configure(): Unit = {}

  @Provides
  @Singleton
  def provideSignalManager(): UnixSignalManager = new UnixSignalManager()
}
