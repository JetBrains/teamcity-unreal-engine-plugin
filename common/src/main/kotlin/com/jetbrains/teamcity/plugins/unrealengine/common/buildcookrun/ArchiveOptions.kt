package com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun

import com.jetbrains.teamcity.plugins.unrealengine.common.CommandExecutionContext

data class ArchiveOptions(
    val archiveDirectory: String? = null,
) {
    companion object {
        fun from(runnerParameters: Map<String, String>) = ArchiveOptions(runnerParameters[ArchiveDirectoryParameter.name])
    }

    context(context: CommandExecutionContext)
    fun toArguments() =
        buildList {
            add("-archive")

            archiveDirectory?.let {
                add("-archivedirectory=${context.resolveUserPath(it)}")
            }
        }
}
