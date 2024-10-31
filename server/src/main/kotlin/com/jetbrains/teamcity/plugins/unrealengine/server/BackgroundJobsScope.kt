package com.jetbrains.teamcity.plugins.unrealengine.server

import jetbrains.buildServer.serverSide.BuildServerAdapter
import jetbrains.buildServer.util.executors.ExecutorsFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel

class BackgroundJobsScope :
    BuildServerAdapter(),
    CoroutineScope {
    // For now, we use this only with suspending code.
    // To conserve resources, we stick with a single thread.
    private val scope =
        CoroutineScope(
            ExecutorsFactory
                .newFixedDaemonExecutor("unreal-engine.background-jobs", 1)
                .asCoroutineDispatcher(),
        )

    override fun serverShutdown() {
        scope.cancel("TeamCity server is stopping")
    }

    override val coroutineContext = scope.coroutineContext
}
