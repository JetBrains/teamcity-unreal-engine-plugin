package com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun

import com.jetbrains.teamcity.plugins.unrealengine.common.parameters.CheckboxParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.parameters.TextInputParameter

object StageStageSwitchParameter : CheckboxParameter {
    override val description = "Perform staging and put this build (the executables and content) in a stage directory"
    override val name = "build-cook-run-stage"
    override val displayName = "Stage"
    override val defaultValue = true.toString()
    override val advanced = false

    fun parseStageOptions(runnerParameters: Map<String, String>) =
        runnerParameters[name]?.toBooleanStrictOrNull()?.let {
            StageOptions.from(runnerParameters)
        }
}

object StagingDirectoryParameter : TextInputParameter {
    override val name = "build-cook-run-staging-directory"
    override val displayName = "Staging directory"
    override val defaultValue = ""
    override val required = false
    override val description = "Directory to copy the builds to. If empty, the default one is used \"Saved/StagedBuilds\""
    override val supportsVcsNavigation = false
    override val expandable = false
    override val advanced = true
}

object UsePakParameter : CheckboxParameter {
    override val name = "build-cook-run-use-pak"
    override val displayName = "Pak"
    override val defaultValue = true.toString()
    override val description =
        """
        Put all assets into a single .pak file instead of copying out all the individual files.
        If your project uses a lot of asset files, then using a Pak file may make it easier to distribute as it reduces the amount of files you need to transfer.
        """.trimIndent()
    override val advanced = true
}

object CompressedContentParameter : CheckboxParameter {
    override val name = "build-cook-run-compressed-content"
    override val displayName = "Compressed"
    override val defaultValue = true.toString()
    override val description = "Generate compressed content (decreased deployment size, but potentially take longer to load)."
    override val advanced = true
}

object PrerequisitesParameter : CheckboxParameter {
    override val name = "build-cook-run-prerequisites"
    override val displayName = "Prerequisites"
    override val defaultValue = true.toString()
    override val description =
        """
        Specifies whether to include an installer for prerequisites of packaged games,
        such as redistributable operating system components, on platforms that support it.
        """.trimIndent()
    override val advanced = true
}
