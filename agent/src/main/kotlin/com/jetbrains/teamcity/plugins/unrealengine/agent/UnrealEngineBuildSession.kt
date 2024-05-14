package com.jetbrains.teamcity.plugins.unrealengine.agent

import arrow.core.Either
import arrow.core.raise.either
import com.jetbrains.teamcity.plugins.framework.common.TeamCityLoggers
import jetbrains.buildServer.RunBuildException
import jetbrains.buildServer.agent.BuildFinishedStatus
import jetbrains.buildServer.agent.runner.CommandExecution
import jetbrains.buildServer.agent.runner.MultiCommandBuildSession
import kotlinx.coroutines.runBlocking

class UnrealEngineBuildSession(
    private val workflowCreator: WorkflowCreator,
    private val unrealBuildContext: UnrealBuildContext,
) : MultiCommandBuildSession {

    companion object {
        private val logger = TeamCityLoggers.agent<UnrealEngineBuildSession>()
    }

    private lateinit var workflow: Workflow

    private val executingCommands = ArrayDeque<UnrealEngineCommandExecution>(1)
    private val exitCodes = mutableListOf<Int>()

    override fun sessionStarted() = runBlocking {
        with(unrealBuildContext) {
            workflow = when (val result = either { workflowCreator.create() }) {
                is Either.Left -> {
                    logger.error("There was an error during workflow construction: ${result.value.message}")
                    throw RunBuildException("Workflow cannot be created. Error: ${result.value.message}")
                }
                is Either.Right -> result.value
            }
        }
    }

    override fun getNextCommand(): CommandExecution? {
        processPreviousCommandCompletion()

        val nextCommand = workflow.next()

        if (nextCommand != null) {
            executingCommands.addLast(nextCommand)
        }

        return nextCommand
    }

    override fun sessionFinished(): BuildFinishedStatus {
        processPreviousCommandCompletion()

        return workflow.onCompletion(unrealBuildContext, exitCodes)
    }

    private fun processPreviousCommandCompletion() {
        executingCommands.removeLastOrNull()?.let {
            when (val state = it.state) {
                is UnrealEngineCommandState.Finished -> exitCodes.add(state.exitCode)
                else -> {
                    logger.warn(
                        "Next session command has been requested, but the previous one hasn't been completed yet." +
                            " Previous command state: $state",
                    )
                }
            }
        }
    }
}
