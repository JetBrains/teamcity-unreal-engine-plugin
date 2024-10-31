package com.jetbrains.teamcity.plugins.unrealengine.agent

import com.jetbrains.teamcity.plugins.unrealengine.agent.build.events.BuildStepExecutionMonitor
import jetbrains.buildServer.agent.impl.AgentEventDispatcher

class AgentExtensionPointsRegistration(
    private val eventDispatcher: AgentEventDispatcher,
    private val buildEventNotifier: BuildStepExecutionMonitor,
) {
    fun register() {
        eventDispatcher.addListener(buildEventNotifier)
    }
}
