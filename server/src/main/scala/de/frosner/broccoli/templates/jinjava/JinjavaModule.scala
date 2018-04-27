package de.frosner.broccoli.templates.jinjava

import javax.inject.Singleton

import com.google.inject.{AbstractModule, Provides}
import com.hubspot.jinjava.JinjavaConfig
import de.frosner.broccoli.BroccoliConfiguration
import net.codingwell.scalaguice.ScalaModule

/**
  * Provide JinjavaConfig for the template renderer
  */
class JinjavaModule extends AbstractModule with ScalaModule {
  override def configure(): Unit = {}
  @Provides
  @Singleton
  def provideJinjavaConfig(broccoliConfiguration: BroccoliConfiguration): JinjavaConfig = {
    val config = broccoliConfiguration.templates.jinjava
    JinjavaConfig
      .newBuilder()
      .withMaxRenderDepth(config.maxRenderDepth)
      .withTrimBlocks(config.trimBlocks)
      .withLstripBlocks(config.lstripBlocks)
      .withEnableRecursiveMacroCalls(config.enableRecursiveMacroCalls)
      .withReadOnlyResolver(config.readOnlyResolver)
      .withMaxOutputSize(config.maxOutputSize)
      .withNestedInterpretationEnabled(config.nestedInterpretationEnabled)
      .withFailOnUnknownTokens(config.failOnUnknownTokens)
      .build()
  }
}
