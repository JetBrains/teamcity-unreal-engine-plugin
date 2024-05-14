package com.jetbrains.teamcity.plugins.unrealengine.server.runner

import jetbrains.buildServer.serverSide.RunTypeRegistry

class RunTypeRegistration(
    registry: RunTypeRegistry,
    unrealRunType: UnrealEngineRunType,
) {
    init {
        registry.registerRunType(unrealRunType)
    }
}
