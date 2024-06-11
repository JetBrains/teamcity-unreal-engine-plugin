package com.jetbrains.teamcity.plugins.unrealengine.agent.runautomation

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
import com.jetbrains.teamcity.plugins.unrealengine.agent.WorkflowCreationError
import com.jetbrains.teamcity.plugins.unrealengine.agent.WorkflowCreator
import com.jetbrains.teamcity.plugins.unrealengine.agent.reporting.AutomationTestLogMessageHandler
import com.jetbrains.teamcity.plugins.unrealengine.common.automation.RunAutomationCommand

class RunAutomationWorkflowCreator(
    private val toolRegistry: UnrealToolRegistry,
    private val environment: Environment,
) : WorkflowCreator {
    companion object {
        private val logger = TeamCityLoggers.agent<RunAutomationWorkflowCreator>()
    }

    context(Raise<WorkflowCreationError>, UnrealBuildContext)
    override suspend fun create(): Workflow = Workflow(getCommands())

    context(Raise<WorkflowCreationError>, UnrealBuildContext)
    private suspend fun getCommands() =
        listOf(
            runAutomation(),
        )

    context(Raise<WorkflowCreationError>, UnrealBuildContext)
    private suspend fun runAutomation(): UnrealEngineCommandExecution {
        val command =
            either { RunAutomationCommand.from(runnerParameters) }.getOrElse {
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
                toolRegistry.editor(runnerParameters).executablePath,
                arguments,
            ),
            UnrealEngineProcessListener.create(AutomationTestLogMessageHandler()),
        )
    }
}
