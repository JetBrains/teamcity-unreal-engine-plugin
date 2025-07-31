package com.jetbrains.teamcity.plugins.unrealengine.agent.buildgraph

import arrow.core.raise.Raise
import com.jetbrains.teamcity.plugins.framework.common.Environment
import com.jetbrains.teamcity.plugins.unrealengine.agent.UnrealBuildContext
import com.jetbrains.teamcity.plugins.unrealengine.agent.UnrealEngineCommandExecution
import com.jetbrains.teamcity.plugins.unrealengine.agent.UnrealEngineProgramCommandLine
import com.jetbrains.teamcity.plugins.unrealengine.agent.UnrealToolRegistry
import com.jetbrains.teamcity.plugins.unrealengine.agent.build.log.UnrealEngineProcessListenerFactory
import com.jetbrains.teamcity.plugins.unrealengine.agent.reporting.AutomationTestLogEventHandler
import com.jetbrains.teamcity.plugins.unrealengine.common.GenericError
import com.jetbrains.teamcity.plugins.unrealengine.common.buildgraph.BuildGraphCommand

class SingleMachineExecutor(
    private val environment: Environment,
    private val toolRegistry: UnrealToolRegistry,
    private val processListenerFactory: UnrealEngineProcessListenerFactory,
) {
    context(_: Raise<GenericError>, context: UnrealBuildContext)
    suspend fun execute(command: BuildGraphCommand): List<UnrealEngineCommandExecution> =
        listOf(
            UnrealEngineCommandExecution(
                UnrealEngineProgramCommandLine(
                    environment,
                    context.buildParameters.environmentVariables,
                    context.workingDirectory,
                    toolRegistry.automationTool(context.runnerParameters).executablePath,
                    command.toArguments(),
                ),
                processListenerFactory.create(AutomationTestLogEventHandler(context)),
            ),
        )
}
