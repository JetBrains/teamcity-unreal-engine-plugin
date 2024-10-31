package com.jetbrains.teamcity.plugins.unrealengine.server.build.state

import com.jetbrains.teamcity.plugins.unrealengine.server.EventBus
import com.jetbrains.teamcity.plugins.unrealengine.server.EventBusConfig
import com.jetbrains.teamcity.plugins.unrealengine.server.EventBusConsumer
import jetbrains.buildServer.web.functions.InternalProperties
import kotlinx.coroutines.CoroutineScope

interface DistributedBuildStateChangedEventHandler : EventBusConsumer<DistributedBuildStateChanged>

class DistributedBuildEventBusSettings {
    val workerCount: Int
        get() = InternalProperties.getInteger("teamcity.internal.unreal-engine.distributed-build-event-bus.worker-count", 3)

    val workerBufferSize: Int
        get() = InternalProperties.getInteger("teamcity.internal.unreal-engine.distributed-build-event-bus.worker-buffer-size", 100)
}

class DistributedBuildStateChangedEventBus(
    scope: CoroutineScope,
    settings: DistributedBuildEventBusSettings,
    handlers: List<DistributedBuildStateChangedEventHandler>,
) {
    private val eventBus =
        EventBus(
            EventBusConfig(
                name = "Distributed BuildGraph Build Events",
                workerCount = settings.workerCount,
                workerBufferSize = settings.workerBufferSize,
            ),
            scope,
            handlers,
            partitioner = { it.buildId.hashCode() },
        )

    suspend fun dispatch(event: DistributedBuildStateChanged) = eventBus.dispatch(event)
}
