package com.jetbrains.teamcity.plugins.unrealengine.agent.buildcookrun

import arrow.core.getOrElse
import arrow.core.raise.Raise
import arrow.core.raise.either
import com.jetbrains.teamcity.plugins.framework.common.Environment
import com.jetbrains.teamcity.plugins.framework.common.TeamCityLoggers
import com.jetbrains.teamcity.plugins.unrealengine.agent.*
import com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun.BuildCookRunCommand

class BuildCookRunWorkflowCreator(
    private val toolRegistry: UnrealToolRegistry,
    private val environment: Environment,
) : WorkflowCreator {
    companion object {
        private val logger = TeamCityLoggers.agent<BuildCookRunWorkflowCreator>()
    }

    context(Raise<WorkflowCreationError>, UnrealBuildContext)
    override suspend fun create(): Workflow = Workflow(getCommands())

    context(Raise<WorkflowCreationError>, UnrealBuildContext)
    private suspend fun getCommands() =
        listOf(
            buildCookRun(),
        )

    context(Raise<WorkflowCreationError>, UnrealBuildContext)
    private suspend fun buildCookRun(): UnrealEngineCommandExecution {
        val command =
            either { BuildCookRunCommand.from(runnerParameters) }.getOrElse {
                it.forEach { error ->
                    logger.error("An error occurred during command creation: ${error.message}")
                }
                raise(WorkflowCreationError.CommandCreationError("Unable to create command from the given runner parameters"))
            }

        val arguments = command.toArguments().getOrElse { raise(WorkflowCreationError.ExecutionPreparationError(it.message)) }

        return UnrealEngineCommandExecution(
            UnrealEngineProgramCommandLine(
                environment,
                buildParameters.environmentVariables,
                workingDirectory,
                toolRegistry.automationTool(runnerParameters).executablePath,
                arguments,
            ),
            UnrealEngineProcessListener.create(),
        )
    }
}
