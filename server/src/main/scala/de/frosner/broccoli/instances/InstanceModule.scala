package de.frosner.broccoli.instances

import javax.inject.Singleton

import com.google.inject.{AbstractModule, Provides}
import de.frosner.broccoli.BroccoliConfiguration
import de.frosner.broccoli.templates.TemplateRenderer
import net.codingwell.scalaguice.ScalaModule
import play.api.Logger

/**
  * Provide instance storage and template rendering implementations.
  */
class InstanceModule extends AbstractModule with ScalaModule {
  override def configure(): Unit = {}

  /**
    * Provides the template renderer for instances.
    */
  @Provides
  @Singleton
  def provideTemplateRenderer(config: BroccoliConfiguration): TemplateRenderer =
    new TemplateRenderer
}
