package com.jetbrains.teamcity.plugins.unrealengine.agent

import arrow.core.raise.Raise
import com.jetbrains.teamcity.plugins.unrealengine.common.GenericError
import com.jetbrains.teamcity.plugins.unrealengine.common.UnrealEngineRunner
import jetbrains.buildServer.agent.BuildFinishedStatus
import jetbrains.buildServer.agent.problems.ExitCodeProblemBuilder

data class Workflow(
    val commands: Collection<UnrealEngineCommandExecution>,
    val onCompletion: context(UnrealBuildContext)
    (List<Int>) -> BuildFinishedStatus = { workflowCompleted(it) },
) {
    private val commandsQueue = ArrayDeque(commands)

    companion object {
        context(context: UnrealBuildContext)
        private fun workflowCompleted(commandExitCodes: List<Int>): BuildFinishedStatus =
            if (commandExitCodes.all { it == 0 } || !context.build.failBuildOnExitCode) {
                BuildFinishedStatus.FINISHED_SUCCESS
            } else {
                commandExitCodes.filter { it != 0 }.forEach { reportBuildProblem(it) }
                BuildFinishedStatus.FINISHED_WITH_PROBLEMS
            }

        context(context: UnrealBuildContext)
        private fun reportBuildProblem(nonZeroExitCode: Int) {
            context.build.buildLogger.logBuildProblem(
                ExitCodeProblemBuilder()
                    .setExitCode(nonZeroExitCode)
                    .setRunnerId(context.runnerId)
                    .setRunnerName(context.runnerName)
                    .setRunnerType(UnrealEngineRunner.RUN_TYPE)
                    .build(),
            )
        }
    }

    fun next() = commandsQueue.removeFirstOrNull()
}

interface WorkflowCreator {
    context(_: Raise<GenericError>, context: UnrealBuildContext)
    suspend fun create(): Workflow
}
