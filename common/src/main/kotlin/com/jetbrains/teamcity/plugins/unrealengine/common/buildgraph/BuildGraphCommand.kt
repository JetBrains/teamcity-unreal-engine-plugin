package com.jetbrains.teamcity.plugins.unrealengine.common.buildgraph

import arrow.core.NonEmptyList
import arrow.core.raise.Raise
import com.jetbrains.teamcity.plugins.framework.common.zipOrAccumulate
import com.jetbrains.teamcity.plugins.unrealengine.common.CommandExecutionContext
import com.jetbrains.teamcity.plugins.unrealengine.common.GenericError
import com.jetbrains.teamcity.plugins.unrealengine.common.PropertyValidationError
import com.jetbrains.teamcity.plugins.unrealengine.common.UnrealCommand
import com.jetbrains.teamcity.plugins.unrealengine.common.ensure
import com.jetbrains.teamcity.plugins.unrealengine.common.parameters.AdditionalArgumentsParameter

data class BuildGraphCommand(
    val scriptPath: BuildGraphScriptPath,
    val target: BuildGraphTargetNode,
    val options: List<BuildGraphOption>,
    val mode: BuildGraphMode,
    val extraArguments: List<String> = emptyList(),
) : UnrealCommand {
    companion object {
        context(_: Raise<NonEmptyList<PropertyValidationError>>)
        fun from(runnerParameters: Map<String, String>): BuildGraphCommand =
            zipOrAccumulate(
                { BuildGraphScriptPathParameter.parseScriptPath(runnerParameters) },
                { BuildGraphTargetNodeParameter.parseTargetNode(runnerParameters) },
                { BuildGraphOptionsParameter.parseOptions(runnerParameters) },
                { BuildGraphModeParameter.parse(runnerParameters) },
            ) { path, target, options, modeSettings ->
                BuildGraphCommand(
                    path,
                    target,
                    options,
                    modeSettings,
                    AdditionalArgumentsParameter.parse(runnerParameters),
                )
            }
    }

    context(_: Raise<GenericError>, context: CommandExecutionContext)
    override fun toArguments() =
        buildList {
            add("BuildGraph")

            val resolvedScriptPath = context.resolveUserPath(scriptPath.value)
            ensure(
                context.fileExists(resolvedScriptPath),
                "Could not find the specified BuildGraph script file. Path: $resolvedScriptPath",
            )

            add("-script=$resolvedScriptPath")
            add("-target=${target.value}")
            options.forEach {
                add("-set:${it.name}=${it.value}")
            }

            addAll(extraArguments)
        }
}
