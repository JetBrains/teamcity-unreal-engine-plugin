package com.jetbrains.teamcity.plugins.unrealengine.agent.build.events

import com.jetbrains.teamcity.plugins.framework.common.TeamCityLoggers
import com.jetbrains.teamcity.plugins.unrealengine.common.UnrealEngineRunner
import com.jetbrains.teamcity.plugins.unrealengine.common.build.events.AgentBuildEvent
import com.jetbrains.teamcity.plugins.unrealengine.common.build.events.AgentBuildEvent.*
import com.jetbrains.teamcity.plugins.unrealengine.common.build.events.AgentBuildEventConverter
import com.jetbrains.teamcity.plugins.unrealengine.common.build.events.RunnerInternalParameters
import com.jetbrains.teamcity.plugins.unrealengine.common.build.events.StepOutcome
import jetbrains.buildServer.agent.AgentLifeCycleAdapter
import jetbrains.buildServer.agent.AgentRuntimeProperties.TEAMCITY_BUILD_STEP_NAME
import jetbrains.buildServer.agent.BuildFinishedStatus
import jetbrains.buildServer.agent.BuildProgressLogger
import jetbrains.buildServer.agent.BuildRunnerContext
import jetbrains.buildServer.messages.serviceMessages.ServiceMessage

class BuildStepExecutionMonitor(
    private val eventConverter: AgentBuildEventConverter,
) : AgentLifeCycleAdapter() {
    companion object {
        private val logger = TeamCityLoggers.agent<BuildStepExecutionMonitor>()
    }

    override fun beforeRunnerStart(runner: BuildRunnerContext) {
        if (!shouldNotifyAboutExecution(runner)) {
            return
        }

        val stepName = runner.getStepName() ?: return

        runner.build.buildLogger.logUnrealBuildEvent(BuildStepStarted(stepName))
    }

    override fun runnerFinished(
        runner: BuildRunnerContext,
        status: BuildFinishedStatus,
    ) {
        if (!shouldNotifyAboutExecution(runner)) {
            return
        }

        val stepName = runner.getStepName() ?: return

        val event =
            when (status) {
                BuildFinishedStatus.INTERRUPTED -> BuildStepInterrupted(stepName)
                BuildFinishedStatus.FINISHED_SUCCESS -> BuildStepCompleted(stepName, StepOutcome.Success)
                BuildFinishedStatus.FINISHED_FAILED,
                BuildFinishedStatus.FINISHED_WITH_PROBLEMS,
                -> BuildStepCompleted(stepName, StepOutcome.Failure)
                else -> {
                    logger.warn("Received unexpected build finish status: $status. Doing nothing")
                    return
                }
            }

        runner.build.buildLogger.logUnrealBuildEvent(event)
    }

    private fun BuildProgressLogger.logUnrealBuildEvent(event: AgentBuildEvent) {
        message(
            ServiceMessage.asString(
                AgentBuildEventConverter.SERVICE_MESSAGE_NAME,
                eventConverter.toMap(event),
            ),
        )
    }

    private fun shouldNotifyAboutExecution(runner: BuildRunnerContext) =
        runner.runType == UnrealEngineRunner.RUN_TYPE &&
            runner.runnerParameters[RunnerInternalParameters.BUILD_STEP_NOTIFICATIONS_ENABLED].toBoolean()

    /**
     * [BuildRunnerContext.getName] appends run type name to the actual step name, this property returns what you expect
     */
    private fun BuildRunnerContext.getStepName(): String? =
        configParameters[TEAMCITY_BUILD_STEP_NAME].also {
            if (it == null) {
                logger.warn("Runner is missing its step name config parameter, the notification won’t be sent")
            }
        }
}
