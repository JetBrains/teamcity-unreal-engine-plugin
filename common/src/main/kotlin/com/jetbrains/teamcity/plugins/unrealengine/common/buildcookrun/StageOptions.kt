package com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun

import com.jetbrains.teamcity.plugins.unrealengine.common.CommandExecutionContext

data class StageOptions(
    val stagingDirectory: String? = null,
    val usePak: Boolean = false,
    val compressContent: Boolean = false,
    val installPrerequisites: Boolean = false,
) {
    companion object {
        fun from(runnerParameters: Map<String, String>): StageOptions =
            StageOptions(
                runnerParameters[StagingDirectoryParameter.name],
                runnerParameters[UsePakParameter.name].toBoolean(),
                runnerParameters[CompressedContentParameter.name].toBoolean(),
                runnerParameters[PrerequisitesParameter.name].toBoolean(),
            )
    }

    context(context: CommandExecutionContext)
    fun toArguments() =
        buildList {
            add("-stage")

            stagingDirectory?.let {
                add("-stagingdirectory=${context.resolveUserPath(it)}")
            }

            if (usePak) {
                add("-pak")
            }

            if (compressContent) {
                add("-compressed")
            }

            if (installPrerequisites) {
                add("-prereqs")
            }
        }
}
