package com.jetbrains.teamcity.plugins.unrealengine.server.runner.ui

import com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun.ArchiveDirectoryParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun.ArchiveSwitchParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun.CompressedContentParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun.PackageStageSwitchParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun.PrerequisitesParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun.StageStageSwitchParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun.StagingDirectoryParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun.UsePakParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.parameters.CheckboxParameter

class RunComponent {
    val performStage = StageStageSwitchParameter
    val stagingDirectory = StagingDirectoryParameter
    val usePak = UsePakParameter
    val compressedContent = CompressedContentParameter
    val prerequisites = PrerequisitesParameter

    val performPackage = PackageStageSwitchParameter

    val archiveBuild = ArchiveSwitchParameter
    val archiveDirectory = ArchiveDirectoryParameter

    fun formatFlags(properties: Map<String, String>) =
        ComponentParametersFormatter
            .formatFlags(
                sequenceOf(
                    usePak,
                    compressedContent,
                    prerequisites,
                ),
                properties,
            )

    fun formatFlag(
        flag: CheckboxParameter,
        properties: Map<String, String>,
    ) = ComponentParametersFormatter
        .formatFlag(flag, properties)
}
