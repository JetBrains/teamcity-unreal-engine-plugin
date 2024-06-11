package com.jetbrains.teamcity.plugins.unrealengine.server

import com.jetbrains.teamcity.plugins.unrealengine.server.buildgraph.BuildGraphBuildChainCreator
import jetbrains.buildServer.serverSide.SBuildServer

class ServerLifecycleListenerRegistration(
    private val server: SBuildServer,
    private val buildGraphBuildChainCreator: BuildGraphBuildChainCreator,
) {
    fun register() {
        server.addListener(buildGraphBuildChainCreator)
    }
}
