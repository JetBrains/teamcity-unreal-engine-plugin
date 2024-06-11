package com.jetbrains.teamcity.plugins.unrealengine.common.buildgraph

import arrow.core.Either
import arrow.core.NonEmptyList
import arrow.core.raise.Raise
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.zipOrAccumulate
import com.jetbrains.teamcity.plugins.unrealengine.common.ArgumentsPreparationError
import com.jetbrains.teamcity.plugins.unrealengine.common.CommandExecutionContext
import com.jetbrains.teamcity.plugins.unrealengine.common.UnrealCommand
import com.jetbrains.teamcity.plugins.unrealengine.common.ValidationError
import com.jetbrains.teamcity.plugins.unrealengine.common.parameters.AdditionalArgumentsParameter

data class BuildGraphCommand(
    val scriptPath: BuildGraphScriptPath,
    val target: BuildGraphTargetNode,
    val options: List<BuildGraphOption>,
    val mode: BuildGraphMode,
    val extraArguments: List<String> = emptyList(),
) : UnrealCommand {
    companion object {
        context(Raise<NonEmptyList<ValidationError>>)
        fun from(runnerParameters: Map<String, String>): BuildGraphCommand =
            zipOrAccumulate(
                { BuildGraphScriptPathParameter.parseScriptPath(runnerParameters) },
                { BuildGraphTargetNodeParameter.parseTargetNode(runnerParameters) },
                { BuildGraphOptionsParameter.parseOptions(runnerParameters) },
            ) { path, target, options ->
                BuildGraphCommand(
                    path,
                    target,
                    options,
                    BuildGraphModeParameter.parse(runnerParameters),
                    AdditionalArgumentsParameter.parse(runnerParameters),
                )
            }
    }

    context(CommandExecutionContext)
    override fun toArguments(): Either<ArgumentsPreparationError, List<String>> =
        either {
            buildList {
                add("BuildGraph")

                val resolvedScriptPath = concatPaths(workingDirectory, scriptPath.value)
                ensure(fileExists(resolvedScriptPath)) {
                    ArgumentsPreparationError("Could not find the specified BuildGraph script file. Path: $resolvedScriptPath")
                }

                add("-script=$resolvedScriptPath")
                add("-target=${target.value}")
                options.forEach {
                    add("-set:${it.name}=${it.value}")
                }

                addAll(extraArguments)
            }
        }
}
