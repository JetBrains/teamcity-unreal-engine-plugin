package com.jetbrains.teamcity.plugins.unrealengine.agent.buildgraph

import arrow.core.getOrElse
import arrow.core.raise.Raise
import arrow.core.raise.either
import com.jetbrains.teamcity.plugins.unrealengine.agent.UnrealBuildContext
import com.jetbrains.teamcity.plugins.unrealengine.agent.UnrealEngineCommandExecution
import com.jetbrains.teamcity.plugins.unrealengine.agent.Workflow
import com.jetbrains.teamcity.plugins.unrealengine.agent.WorkflowCreator
import com.jetbrains.teamcity.plugins.unrealengine.common.GenericError
import com.jetbrains.teamcity.plugins.unrealengine.common.UnrealPluginLoggers
import com.jetbrains.teamcity.plugins.unrealengine.common.buildgraph.BuildGraphCommand
import com.jetbrains.teamcity.plugins.unrealengine.common.buildgraph.BuildGraphMode
import com.jetbrains.teamcity.plugins.unrealengine.common.raise

class BuildGraphWorkflowCreator(
    private val singleMachineExecutor: SingleMachineExecutor,
    private val distributedExecutor: DistributedExecutor,
) : WorkflowCreator {
    companion object {
        private val logger = UnrealPluginLoggers.get<BuildGraphWorkflowCreator>()
    }

    context(_: Raise<GenericError>, context: UnrealBuildContext)
    override suspend fun create(): Workflow = Workflow(getCommands())

    context(_: Raise<GenericError>, context: UnrealBuildContext)
    private suspend fun getCommands(): List<UnrealEngineCommandExecution> {
        val command =
            either { BuildGraphCommand.from(context.runnerParameters) }.getOrElse {
                it.forEach { error ->
                    logger.error("An error occurred during command creation: ${error.message}")
                }
                raise("Unable to create command from the given runner parameters")
            }

        return when (command.mode) {
            is BuildGraphMode.SingleMachine -> singleMachineExecutor.execute(command)
            is BuildGraphMode.Distributed -> distributedExecutor.execute(command)
        }
    }
}
