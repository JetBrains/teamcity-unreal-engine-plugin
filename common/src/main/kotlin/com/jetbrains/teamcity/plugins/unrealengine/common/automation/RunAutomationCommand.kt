package com.jetbrains.teamcity.plugins.unrealengine.common.automation

import arrow.core.Either
import arrow.core.NonEmptyList
import arrow.core.raise.Raise
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.zipOrAccumulate
import com.jetbrains.teamcity.plugins.unrealengine.common.ArgumentsPreparationError
import com.jetbrains.teamcity.plugins.unrealengine.common.CommandExecutionContext
import com.jetbrains.teamcity.plugins.unrealengine.common.PropertyValidationError
import com.jetbrains.teamcity.plugins.unrealengine.common.UnrealCommand
import com.jetbrains.teamcity.plugins.unrealengine.common.UnrealProjectPath
import com.jetbrains.teamcity.plugins.unrealengine.common.parameters.AdditionalArgumentsParameter

@JvmInline
value class UnrealAutomationTest(
    val value: String,
)

enum class RunFilterType {
    Engine,
    Smoke,
    Stress,
    Perf,
    Product,
}

sealed interface ExecCommand {
    data object RunAll : ExecCommand

    data class RunFilter(
        val filter: RunFilterType,
    ) : ExecCommand

    data class RunTests(
        val tests: List<UnrealAutomationTest>,
    ) : ExecCommand
}

data class RunAutomationCommand(
    val projectPath: UnrealProjectPath,
    val nullRHI: Boolean,
    val execCommand: ExecCommand,
    val extraArguments: List<String> = emptyList(),
) : UnrealCommand {
    companion object {
        context(Raise<NonEmptyList<PropertyValidationError>>)
        fun from(runnerParameters: Map<String, String>) =
            zipOrAccumulate(
                { AutomationProjectPathParameter.parseProjectPath(runnerParameters) },
                { AutomationExecCommandParameter.parse(runnerParameters) },
                { AdditionalArgumentsParameter.parse(runnerParameters) },
            ) { projectPath, command, extraArguments ->
                val nullRHI = runnerParameters[NullRHIParameter.name].toBoolean()

                RunAutomationCommand(
                    projectPath,
                    nullRHI,
                    command,
                    extraArguments,
                )
            }
    }

    context(CommandExecutionContext)
    override fun toArguments(): Either<ArgumentsPreparationError, List<String>> =
        either {
            buildList {
                val resolvedProjectPath = concatPaths(workingDirectory, projectPath.value)
                ensure(
                    fileExists(resolvedProjectPath),
                ) { ArgumentsPreparationError("Could not find the specified project file. Path: $resolvedProjectPath") }
                add(resolvedProjectPath)

                if (nullRHI) {
                    add("-nullrhi")
                }

                add(
                    buildString {
                        append("-ExecCmds=Automation ")
                        when (execCommand) {
                            is ExecCommand.RunAll -> append("RunAll;")
                            is ExecCommand.RunFilter -> append("RunFilter ${execCommand.filter.name};")
                            is ExecCommand.RunTests -> {
                                append("RunTests ")
                                append(execCommand.tests.joinToString(separator = "+") { it.value })
                                append(";")
                            }
                        }
                        append("Quit;")
                    },
                )

                addAll(extraArguments)
            }
        }
}
