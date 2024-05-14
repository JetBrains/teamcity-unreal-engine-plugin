package com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun

import com.jetbrains.teamcity.plugins.unrealengine.common.CommandExecutionContext

data class ArchiveOptions(
    val archiveDirectory: String? = null,
) {
    companion object {
        fun from(runnerParameters: Map<String, String>): ArchiveOptions {
            return ArchiveOptions(runnerParameters[ArchiveDirectoryParameter.name])
        }
    }

    context(CommandExecutionContext)
    fun toArguments() = buildList {
        add("-archive")

        if (archiveDirectory != null) {
            add("-archivedirectory=${concatPaths(workingDirectory, archiveDirectory)}")
        }
    }
}
