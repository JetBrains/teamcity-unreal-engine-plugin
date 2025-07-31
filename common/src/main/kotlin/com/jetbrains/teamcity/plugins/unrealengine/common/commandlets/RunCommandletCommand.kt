package com.jetbrains.teamcity.plugins.unrealengine.common.commandlets

import arrow.core.NonEmptyList
import arrow.core.raise.Raise
import com.jetbrains.teamcity.plugins.framework.common.zipOrAccumulate
import com.jetbrains.teamcity.plugins.unrealengine.common.CommandExecutionContext
import com.jetbrains.teamcity.plugins.unrealengine.common.GenericError
import com.jetbrains.teamcity.plugins.unrealengine.common.PropertyValidationError
import com.jetbrains.teamcity.plugins.unrealengine.common.UnrealCommand
import com.jetbrains.teamcity.plugins.unrealengine.common.UnrealProjectPath
import com.jetbrains.teamcity.plugins.unrealengine.common.ensure
import com.jetbrains.teamcity.plugins.unrealengine.common.parameters.AdditionalArgumentsParameter

@JvmInline
value class Commandlet(
    val value: String,
)

class RunCommandletCommand(
    val projectPath: UnrealProjectPath? = null,
    val commandlet: Commandlet,
    val commandletArguments: List<String> = emptyList(),
    val extraArguments: List<String> = emptyList(),
) : UnrealCommand {
    companion object {
        context(_: Raise<NonEmptyList<PropertyValidationError>>)
        fun from(runnerParameters: Map<String, String>): RunCommandletCommand =
            zipOrAccumulate(
                { CommandletProjectPathParameter.parseProjectPath(runnerParameters) },
                { CommandletNameParameter.parseCommandlet(runnerParameters) },
            ) { projectPath, commandlet ->
                RunCommandletCommand(
                    projectPath,
                    commandlet,
                    CommandletArgumentsParameter.parse(runnerParameters),
                    AdditionalArgumentsParameter.parse(runnerParameters),
                )
            }
    }

    context(_: Raise<GenericError>, context: CommandExecutionContext)
    override fun toArguments(): List<String> =
        buildList {
            projectPath?.let {
                val resolvedProjectPath = context.resolveUserPath(it.value)
                ensure(
                    context.fileExists(resolvedProjectPath),
                    "Could not find the specified project file. Path: $resolvedProjectPath",
                )
                add(resolvedProjectPath)
            }

            add("-run=${commandlet.value}")
            addAll(commandletArguments)
            addAll(extraArguments)
        }
}
