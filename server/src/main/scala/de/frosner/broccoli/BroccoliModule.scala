package de.frosner.broccoli

import com.google.inject.{AbstractModule, Provides, Singleton}
import net.codingwell.scalaguice.ScalaModule
import play.api.Configuration
import pureconfig._
import pureconfig.module.enumeratum._

/**
  * Provide basic broccoli globals.
  */
class BroccoliModule extends AbstractModule with ScalaModule {
  override def configure(): Unit = {}

  /**
    * Provide the Broccoli configuration.
    *
    * @param configuration The underlying configuration to load from.
    */
  @Provides
  @Singleton
  def provideConfiguration(configuration: Configuration): BroccoliConfiguration =
    loadConfigOrThrow[BroccoliConfiguration](configuration.underlying.getConfig("broccoli"))
}
