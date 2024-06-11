package com.jetbrains.teamcity.plugins.unrealengine.common.buildgraph

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

enum class BuildGraphMode {
    SingleMachine,
    Distributed,
}
