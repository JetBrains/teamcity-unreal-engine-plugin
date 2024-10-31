package com.jetbrains.teamcity.plugins.unrealengine.server.build.state

import arrow.core.raise.Raise
import com.jetbrains.teamcity.plugins.framework.common.TeamCityLoggers
import com.jetbrains.teamcity.plugins.unrealengine.common.Error
import com.jetbrains.teamcity.plugins.unrealengine.common.build.events.AgentBuildEvent
import com.jetbrains.teamcity.plugins.unrealengine.common.ensureNotNull
import com.jetbrains.teamcity.plugins.unrealengine.server.build.agent.AgentBuildEventHandler
import jetbrains.buildServer.serverSide.BuildsManager

class AgentBuildStateEventMonitor(
    private val stateTracker: DistributedBuildStateTracker,
    private val buildManager: BuildsManager,
) : AgentBuildEventHandler {
    companion object {
        private val logger = TeamCityLoggers.server<AgentBuildStateEventMonitor>()
    }

    context(Raise<Error>)
    override suspend fun handleBuildEvent(
        buildId: Long,
        event: AgentBuildEvent,
    ) {
        if (!(
                event is AgentBuildEvent.BuildStepCompleted ||
                    event is AgentBuildEvent.BuildStepStarted ||
                    event is AgentBuildEvent.BuildStepInterrupted
            )
        ) {
            logger.debug("Skipping agent build event handling since it's not related to build state update")
            return
        }

        val build =
            ensureNotNull(
                buildManager.findBuildInstanceById(buildId),
                "Unable to find a build with the given id $buildId",
            )

        val serverSideEvent =
            when (event) {
                is AgentBuildEvent.BuildStepCompleted ->
                    DistributedBuildEvent.BuildStepCompleted(
                        build,
                        event.name,
                        event.outcome,
                    )
                is AgentBuildEvent.BuildStepStarted -> DistributedBuildEvent.BuildStepStarted(build, event.name)
                is AgentBuildEvent.BuildStepInterrupted -> DistributedBuildEvent.BuildStepInterrupted(build, event.name)
                else -> return
            }

        stateTracker.handleBuildEvent(serverSideEvent)
    }
}
