package com.jetbrains.teamcity.plugins.unrealengine.agent.commandlets

import arrow.core.getOrElse
import arrow.core.raise.Raise
import arrow.core.raise.either
import com.jetbrains.teamcity.plugins.framework.common.Environment
import com.jetbrains.teamcity.plugins.framework.common.TeamCityLoggers
import com.jetbrains.teamcity.plugins.unrealengine.agent.UnrealBuildContext
import com.jetbrains.teamcity.plugins.unrealengine.agent.UnrealEngineCommandExecution
import com.jetbrains.teamcity.plugins.unrealengine.agent.UnrealEngineProcessListener
import com.jetbrains.teamcity.plugins.unrealengine.agent.UnrealEngineProgramCommandLine
import com.jetbrains.teamcity.plugins.unrealengine.agent.UnrealToolRegistry
import com.jetbrains.teamcity.plugins.unrealengine.agent.Workflow
import com.jetbrains.teamcity.plugins.unrealengine.agent.WorkflowCreator
import com.jetbrains.teamcity.plugins.unrealengine.common.GenericError
import com.jetbrains.teamcity.plugins.unrealengine.common.commandlets.RunCommandletCommand
import com.jetbrains.teamcity.plugins.unrealengine.common.raise

class CommandletWorkflowCreator(
    private val toolRegistry: UnrealToolRegistry,
    private val environment: Environment,
) : WorkflowCreator {
    companion object {
        private val logger = TeamCityLoggers.agent<CommandletWorkflowCreator>()
    }

    context(Raise<GenericError>, UnrealBuildContext)
    override suspend fun create() = Workflow(getCommands())

    context(Raise<GenericError>, UnrealBuildContext)
    private suspend fun getCommands(): List<UnrealEngineCommandExecution> =
        listOf(
            run(),
        )

    context(Raise<GenericError>, UnrealBuildContext)
    private suspend fun run(): UnrealEngineCommandExecution {
        val command =
            either { RunCommandletCommand.from(runnerParameters) }.getOrElse {
                it.forEach { error ->
                    logger.error("An error occurred during command creation: ${error.message}")
                }
                raise("Unable to create command from the given runner parameters")
            }

        val arguments = command.toArguments()

        return UnrealEngineCommandExecution(
            UnrealEngineProgramCommandLine(
                environment,
                buildParameters.environmentVariables,
                workingDirectory,
                toolRegistry.editor(runnerParameters).executablePath,
                arguments,
            ),
            UnrealEngineProcessListener.create(),
        )
    }
}