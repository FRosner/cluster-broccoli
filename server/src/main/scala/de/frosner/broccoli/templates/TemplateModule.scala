package de.frosner.broccoli.templates

import javax.inject.{Inject, Singleton}

import com.google.inject.{AbstractModule, Provides}
import de.frosner.broccoli.signal.UnixSignalManager
import net.codingwell.scalaguice.ScalaModule
import play.api.Configuration

/**
  * Provide a template source from the Play configuration
  */
class TemplateModule extends AbstractModule with ScalaModule {
  override def configure(): Unit = {}

  /**
    * Provide the template source.
    *
    * @param config The Play configuration
    * @return The template source configured from the Play configuration
    */
  @Provides
  @Singleton
  @Inject
  def provideTemplateSource(config: Configuration, signalManager: UnixSignalManager): TemplateSource =
    new SignalRefreshedTemplateSource(
      new CachedTemplateSource(
        new DirectoryTemplateSource(
          TemplateConfiguration.fromConfig(config.underlying.getConfig("broccoli.templates")).templatesPath)),
      signalManager
    )

}
