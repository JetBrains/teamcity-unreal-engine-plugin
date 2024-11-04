package com.jetbrains.teamcity.plugins.unrealengine.agent.buildgraph

import arrow.core.raise.Raise
import com.jetbrains.teamcity.plugins.framework.common.Environment
import com.jetbrains.teamcity.plugins.unrealengine.agent.UnrealBuildContext
import com.jetbrains.teamcity.plugins.unrealengine.agent.UnrealEngineCommandExecution
import com.jetbrains.teamcity.plugins.unrealengine.agent.UnrealEngineProcessListener
import com.jetbrains.teamcity.plugins.unrealengine.agent.UnrealEngineProgramCommandLine
import com.jetbrains.teamcity.plugins.unrealengine.agent.UnrealToolRegistry
import com.jetbrains.teamcity.plugins.unrealengine.agent.reporting.AutomationTestLogMessageHandler
import com.jetbrains.teamcity.plugins.unrealengine.common.GenericError
import com.jetbrains.teamcity.plugins.unrealengine.common.buildgraph.BuildGraphCommand

class SingleMachineExecutor(
    private val environment: Environment,
    private val toolRegistry: UnrealToolRegistry,
) {
    context(Raise<GenericError>, UnrealBuildContext)
    suspend fun execute(command: BuildGraphCommand): List<UnrealEngineCommandExecution> =
        listOf(
            UnrealEngineCommandExecution(
                UnrealEngineProgramCommandLine(
                    environment,
                    buildParameters.environmentVariables,
                    workingDirectory,
                    toolRegistry.automationTool(runnerParameters).executablePath,
                    command.toArguments(),
                ),
                UnrealEngineProcessListener.create(AutomationTestLogMessageHandler()),
            ),
        )
}
