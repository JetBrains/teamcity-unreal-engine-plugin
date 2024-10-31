package com.jetbrains.teamcity.plugins.unrealengine.common.buildgraph

import com.jetbrains.teamcity.plugins.unrealengine.common.ugs.UgsMetadataServerUrl

@JvmInline
value class BuildGraphScriptPath(
    val value: String,
)

@JvmInline
value class BuildGraphTargetNode(
    val value: String,
)

data class BuildGraphOption(
    val name: String,
    val value: String,
)

sealed interface BuildGraphMode {
    data object SingleMachine : BuildGraphMode

    data class Distributed(
        val metadataServerUrl: UgsMetadataServerUrl? = null,
    ) : BuildGraphMode
}
