package com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun

import com.jetbrains.teamcity.plugins.unrealengine.common.parameters.CheckboxParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.parameters.TextInputParameter

object PackageStageSwitchParameter : CheckboxParameter {
    override val name = "build-cook-run-package"
    override val displayName = "Package"
    override val defaultValue = true.toString()
    override val description = "Package for the target platform."
    override val advanced = false
}

object ArchiveSwitchParameter : CheckboxParameter {
    override val name = "build-cook-run-archive"
    override val displayName = "Archive"
    override val defaultValue = true.toString()
    override val description = "Place this build in an archive directory."
    override val advanced = false

    fun parseArchiveOptions(runnerParameters: Map<String, String>) =
        runnerParameters[name]?.toBooleanStrictOrNull()?.let {
            ArchiveOptions.from(runnerParameters)
        }
}

object ArchiveDirectoryParameter : TextInputParameter {
    override val name = "build-cook-run-archive-directory"
    override val displayName = "Archive directory"
    override val defaultValue = ""
    override val description = null
    override val required = false
    override val supportsVcsNavigation = false
    override val expandable = false
    override val advanced = true
}
