package com.jetbrains.teamcity.plugins.unrealengine.agent.automation.commands

import arrow.core.getOrElse
import arrow.core.raise.Raise
import arrow.core.raise.either
import com.jetbrains.teamcity.plugins.framework.common.Environment
import com.jetbrains.teamcity.plugins.unrealengine.agent.UnrealBuildContext
import com.jetbrains.teamcity.plugins.unrealengine.agent.UnrealEngineCommandExecution
import com.jetbrains.teamcity.plugins.unrealengine.agent.UnrealEngineProgramCommandLine
import com.jetbrains.teamcity.plugins.unrealengine.agent.UnrealToolRegistry
import com.jetbrains.teamcity.plugins.unrealengine.agent.Workflow
import com.jetbrains.teamcity.plugins.unrealengine.agent.WorkflowCreator
import com.jetbrains.teamcity.plugins.unrealengine.agent.build.log.UnrealEngineProcessListenerFactory
import com.jetbrains.teamcity.plugins.unrealengine.common.GenericError
import com.jetbrains.teamcity.plugins.unrealengine.common.UnrealPluginLoggers
import com.jetbrains.teamcity.plugins.unrealengine.common.automation.commands.RunAutomationCommand
import com.jetbrains.teamcity.plugins.unrealengine.common.raise

class RunAutomationCommandWorkflowCreator(
    private val toolRegistry: UnrealToolRegistry,
    private val environment: Environment,
    private val processListenerFactory: UnrealEngineProcessListenerFactory,
) : WorkflowCreator {
    companion object {
        private val logger = UnrealPluginLoggers.get<RunAutomationCommandWorkflowCreator>()
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
            either { RunAutomationCommand.from(runnerParameters) }.getOrElse {
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
                toolRegistry.automationTool(runnerParameters).executablePath,
                arguments,
            ),
            processListenerFactory.create(),
        )
    }
}
