package com.jetbrains.teamcity.plugins.unrealengine.common.automation.tests

import arrow.core.NonEmptyList
import arrow.core.raise.Raise
import arrow.core.raise.zipOrAccumulate
import com.jetbrains.teamcity.plugins.unrealengine.common.CommandExecutionContext
import com.jetbrains.teamcity.plugins.unrealengine.common.GenericError
import com.jetbrains.teamcity.plugins.unrealengine.common.PropertyValidationError
import com.jetbrains.teamcity.plugins.unrealengine.common.UnrealCommand
import com.jetbrains.teamcity.plugins.unrealengine.common.UnrealProjectPath
import com.jetbrains.teamcity.plugins.unrealengine.common.ensure
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

data class RunAutomationTestsCommand(
    val projectPath: UnrealProjectPath,
    val nullRHI: Boolean,
    val execCommand: ExecCommand,
    val extraArguments: List<String> = emptyList(),
) : UnrealCommand {
    companion object {
        context(Raise<NonEmptyList<PropertyValidationError>>)
        fun from(runnerParameters: Map<String, String>) =
            zipOrAccumulate(
                { AutomationTestsProjectPathParameter.parseProjectPath(runnerParameters) },
                { AutomationTestsExecCommandParameter.parse(runnerParameters) },
                { AdditionalArgumentsParameter.parse(runnerParameters) },
            ) { projectPath, command, extraArguments ->
                val nullRHI = runnerParameters[NullRHIParameter.name].toBoolean()

                RunAutomationTestsCommand(
                    projectPath,
                    nullRHI,
                    command,
                    extraArguments,
                )
            }
    }

    context(Raise<GenericError>, CommandExecutionContext)
    override fun toArguments() =
        buildList {
            val resolvedProjectPath = resolvePath(workingDirectory, projectPath.value)
            ensure(
                fileExists(resolvedProjectPath),
                "Could not find the specified project file. Path: $resolvedProjectPath",
            )
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
