package com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun

import com.jetbrains.teamcity.plugins.unrealengine.common.parameters.TextInputParameter

object UnrealBuildTargetParameter : TextInputParameter {
    override val name = "build-cook-run-build-target"
    override val displayName = "Target"
    override val description = """
        Explicitly specify the build target; this might be useful if your project has multiple targets of the same type.
        You can also join multiple targets using '+'.
    """.trimIndent()
    override val supportsVcsNavigation = false
    override val expandable = false
    override val required = false
    override val advanced = true
    override val defaultValue = ""

    private const val SEPARATOR = "+"

    fun joinBuildTargets(targets: Collection<UnrealBuildTarget>) = targets.joinToString(separator = SEPARATOR) { it.value }

    fun parseBuildTargets(properties: Map<String, String>): List<UnrealBuildTarget> {
        val targets = properties[name] ?: return emptyList()

        return targets.split(SEPARATOR)
            .filter { it.isNotEmpty() }
            .map { UnrealBuildTarget(it.trim()) }
    }
}
