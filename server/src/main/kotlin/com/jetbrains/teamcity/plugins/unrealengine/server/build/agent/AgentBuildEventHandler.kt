package com.jetbrains.teamcity.plugins.unrealengine.server.build.agent

import arrow.core.raise.Raise
import com.jetbrains.teamcity.plugins.unrealengine.common.Error
import com.jetbrains.teamcity.plugins.unrealengine.common.build.events.AgentBuildEvent

interface AgentBuildEventHandler {
    context(_: Raise<Error>)
    suspend fun handleBuildEvent(
        buildId: Long,
        event: AgentBuildEvent,
    )
}
