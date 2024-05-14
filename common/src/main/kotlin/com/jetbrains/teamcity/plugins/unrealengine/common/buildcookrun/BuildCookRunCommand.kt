package com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun

import arrow.core.Either
import arrow.core.NonEmptyList
import arrow.core.raise.Raise
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.zipOrAccumulate
import com.jetbrains.teamcity.plugins.unrealengine.common.ArgumentsPreparationError
import com.jetbrains.teamcity.plugins.unrealengine.common.CommandExecutionContext
import com.jetbrains.teamcity.plugins.unrealengine.common.UnrealCommand
import com.jetbrains.teamcity.plugins.unrealengine.common.UnrealProjectPath
import com.jetbrains.teamcity.plugins.unrealengine.common.ValidationError
import com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun.BuildConfigurationParameter.parseBuildConfiguration
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
        context(Raise<NonEmptyList<ValidationError>>)
        fun from(runnerParameters: Map<String, String>) = zipOrAccumulate(
            { BuildCookRunProjectPathParameter.parseProjectPath(runnerParameters) },
            { parseBuildConfiguration(runnerParameters) },
        ) { projectPath, buildConfiguration ->
            val cookOptions = runnerParameters[CookStageSwitchParameter.name]?.let {
                return@let if (it.toBoolean()) {
                    CookOptions.from(runnerParameters)
                } else {
                    null
                }
            }

            val stageOptions = runnerParameters[StageStageSwitchParameter.name]?.let {
                return@let if (it.toBoolean()) {
                    StageOptions.from(runnerParameters)
                } else {
                    null
                }
            }

            val archiveOptions = runnerParameters[ArchiveSwitchParameter.name]?.let {
                return@let if (it.toBoolean()) {
                    ArchiveOptions.from(runnerParameters)
                } else {
                    null
                }
            }

            val additionalOptions = buildList {
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

    context(CommandExecutionContext)
    override fun toArguments(): Either<ArgumentsPreparationError, List<String>> = either {
        buildList {
            add("BuildCookRun")

            val resolvedProjectPath = concatPaths(workingDirectory, projectPath.value)
            ensure(
                fileExists(resolvedProjectPath),
            ) { ArgumentsPreparationError("Could not find the specified project file. Path: $resolvedProjectPath") }
            add("-project=$resolvedProjectPath")

            addAll(buildType.arguments)

            if (cookOptions != null) {
                addAll(cookOptions.arguments)
            } else {
                add("-skipcook")
            }

            if (stageOptions != null) {
                addAll(stageOptions.toArguments())
            } else {
                add("-skipstage")
            }

            if (archiveOptions != null) {
                addAll(archiveOptions.toArguments())
            }

            addAll(extraArguments)
        }
    }
}
