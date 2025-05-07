package com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun

import arrow.core.NonEmptyList
import arrow.core.raise.Raise
import arrow.core.raise.zipOrAccumulate
import com.jetbrains.teamcity.plugins.unrealengine.common.CommandExecutionContext
import com.jetbrains.teamcity.plugins.unrealengine.common.GenericError
import com.jetbrains.teamcity.plugins.unrealengine.common.PropertyValidationError
import com.jetbrains.teamcity.plugins.unrealengine.common.UnrealCommand
import com.jetbrains.teamcity.plugins.unrealengine.common.UnrealProjectPath
import com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun.BuildConfigurationParameter.parseBuildConfiguration
import com.jetbrains.teamcity.plugins.unrealengine.common.ensure
import com.jetbrains.teamcity.plugins.unrealengine.common.parameters.AdditionalArgumentsParameter

data class BuildCookRunCommand(
    val projectPath: UnrealProjectPath,
    val buildType: BuildConfiguration,
    val cookOptions: CookOptions? = null,
    val stageOptions: StageOptions? = null,
    val archiveOptions: ArchiveOptions? = null,
    val extraArguments: List<String> = emptyList(),
) : UnrealCommand {
    companion object {
        context(Raise<NonEmptyList<PropertyValidationError>>)
        fun from(runnerParameters: Map<String, String>) =
            zipOrAccumulate(
                { BuildCookRunProjectPathParameter.parseProjectPath(runnerParameters) },
                { parseBuildConfiguration(runnerParameters) },
            ) { projectPath, buildConfiguration ->
                val cookOptions = CookStageSwitchParameter.parseCookOptions(runnerParameters)
                val stageOptions = StageStageSwitchParameter.parseStageOptions(runnerParameters)
                val archiveOptions = ArchiveSwitchParameter.parseArchiveOptions(runnerParameters)

                val additionalOptions =
                    buildList {
                        if (runnerParameters[PackageStageSwitchParameter.name].toBoolean()) {
                            add("-package")
                        }

                        addAll(AdditionalArgumentsParameter.parse(runnerParameters))
                    }

                BuildCookRunCommand(
                    projectPath,
                    buildConfiguration,
                    cookOptions,
                    stageOptions,
                    archiveOptions,
                    additionalOptions,
                )
            }
    }

    context(Raise<GenericError>, CommandExecutionContext)
    override fun toArguments() =
        buildList {
            add("BuildCookRun")

            val resolvedProjectPath = resolveUserPath(projectPath.value)
            ensure(
                fileExists(resolvedProjectPath),
                "Could not find the specified project file. Path: $resolvedProjectPath",
            )
            add("-project=$resolvedProjectPath")

            addAll(buildType.arguments)

            cookOptions?.let { addAll(cookOptions.arguments) } ?: add("-skipcook")
            stageOptions?.let { addAll(stageOptions.toArguments()) } ?: add("-skipstage")
            archiveOptions?.let { addAll(archiveOptions.toArguments()) }

            addAll(extraArguments)
        }
}
