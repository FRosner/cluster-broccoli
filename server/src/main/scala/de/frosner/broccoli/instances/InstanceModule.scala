package de.frosner.broccoli.instances

import java.nio.file.FileSystems
import javax.inject.Singleton

import com.google.inject.{AbstractModule, Provides}
import net.codingwell.scalaguice.ScalaModule
import play.api.Configuration

class InstanceModule extends AbstractModule with ScalaModule {
  override def configure(): Unit = {}

  @Provides
  @Singleton
  def provideInstanceConfiguration(config: Configuration): InstanceConfiguration =
    InstanceConfiguration.fromConfig(config.underlying.getConfig("broccoli.instances"))
}
