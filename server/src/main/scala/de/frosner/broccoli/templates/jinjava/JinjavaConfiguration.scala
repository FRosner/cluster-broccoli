package de.frosner.broccoli.templates.jinjava

import com.hubspot.jinjava.JinjavaConfig

final case class JinjavaConfiguration(
    maxRenderDepth: Int = new JinjavaConfig().getMaxRenderDepth,
    trimBlocks: Boolean = new JinjavaConfig().isTrimBlocks,
    lstripBlocks: Boolean = new JinjavaConfig().isLstripBlocks,
    readOnlyResolver: Boolean = new JinjavaConfig().isReadOnlyResolver,
    enableRecursiveMacroCalls: Boolean = new JinjavaConfig().isEnableRecursiveMacroCalls,
    failOnUnknownTokens: Boolean = new JinjavaConfig().isFailOnUnknownTokens,
    maxOutputSize: Long = new JinjavaConfig().getMaxOutputSize,
    nestedInterpretationEnabled: Boolean = new JinjavaConfig().isNestedInterpretationEnabled)
