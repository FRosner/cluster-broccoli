package de.frosner.broccoli.templates.jinjava

import com.hubspot.jinjava.JinjavaConfig
import JinjavaConfiguration.defaultJinJavaConfig

final case class JinjavaConfiguration(
    maxRenderDepth: Int = defaultJinJavaConfig.getMaxRenderDepth,
    trimBlocks: Boolean = defaultJinJavaConfig.isTrimBlocks,
    lstripBlocks: Boolean = defaultJinJavaConfig.isLstripBlocks,
    readOnlyResolver: Boolean = defaultJinJavaConfig.isReadOnlyResolver,
    enableRecursiveMacroCalls: Boolean = defaultJinJavaConfig.isEnableRecursiveMacroCalls,
    failOnUnknownTokens: Boolean = defaultJinJavaConfig.isFailOnUnknownTokens,
    maxOutputSize: Long = defaultJinJavaConfig.getMaxOutputSize,
    nestedInterpretationEnabled: Boolean = defaultJinJavaConfig.isNestedInterpretationEnabled)

object JinjavaConfiguration {
  val defaultJinJavaConfig = new JinjavaConfig()
}
