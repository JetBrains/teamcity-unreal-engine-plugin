package com.jetbrains.teamcity.plugins.unrealengine.agent.automation.tests

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
import com.jetbrains.teamcity.plugins.unrealengine.agent.reporting.AutomationTestLogEventHandler
import com.jetbrains.teamcity.plugins.unrealengine.common.GenericError
import com.jetbrains.teamcity.plugins.unrealengine.common.UnrealPluginLoggers
import com.jetbrains.teamcity.plugins.unrealengine.common.automation.tests.RunAutomationTestsCommand
import com.jetbrains.teamcity.plugins.unrealengine.common.raise

class RunAutomationTestsWorkflowCreator(
    private val toolRegistry: UnrealToolRegistry,
    private val environment: Environment,
    private val processListenerFactory: UnrealEngineProcessListenerFactory,
) : WorkflowCreator {
    companion object {
        private val logger = UnrealPluginLoggers.get<RunAutomationTestsWorkflowCreator>()
    }

    context(_: Raise<GenericError>, context: UnrealBuildContext)
    override suspend fun create(): Workflow = Workflow(getCommands())

    context(_: Raise<GenericError>, context: UnrealBuildContext)
    private suspend fun getCommands() =
        listOf(
            runAutomationTests(),
        )

    context(_: Raise<GenericError>, context: UnrealBuildContext)
    private suspend fun runAutomationTests(): UnrealEngineCommandExecution {
        val command =
            either { RunAutomationTestsCommand.from(context.runnerParameters) }.getOrElse {
                it.forEach { error ->
                    logger.error("An error occurred during command creation: ${error.message}")
                }
                raise("Unable to create command from the given runner parameters")
            }

        val arguments = command.toArguments()

        return UnrealEngineCommandExecution(
            UnrealEngineProgramCommandLine(
                environment,
                context.buildParameters.environmentVariables,
                context.workingDirectory,
                toolRegistry.editor(context.runnerParameters).executablePath,
                arguments,
            ),
            processListenerFactory.create(AutomationTestLogEventHandler(context)),
        )
    }
}
