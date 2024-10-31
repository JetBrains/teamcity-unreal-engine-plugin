package com.jetbrains.teamcity.plugins.unrealengine.server

import com.jetbrains.teamcity.plugins.unrealengine.server.build.agent.AgentBuildEventReceiver
import com.jetbrains.teamcity.plugins.unrealengine.server.build.state.SkippedBuildMonitor
import com.jetbrains.teamcity.plugins.unrealengine.server.buildgraph.BuildGraphSetupBuildListener
import jetbrains.buildServer.serverSide.SBuildServer
import jetbrains.buildServer.serverSide.ServerExtensionHolder

class ServerExtensionPointsRegistration(
    private val server: SBuildServer,
    private val extensions: ServerExtensionHolder,
    private val buildGraphSetupBuildListener: BuildGraphSetupBuildListener,
    private val agentBuildEventReceiver: AgentBuildEventReceiver,
    private val buildEventMonitor: SkippedBuildMonitor,
    private val backgroundJobsScope: BackgroundJobsScope,
) {
    fun register() {
        server.addListener(buildGraphSetupBuildListener)
        server.addListener(backgroundJobsScope)
        server.addListener(buildEventMonitor)
        extensions.registerExtension(
            AgentBuildEventReceiver::class.java,
            AgentBuildEventReceiver::class.java.name,
            agentBuildEventReceiver,
        )
    }
}
