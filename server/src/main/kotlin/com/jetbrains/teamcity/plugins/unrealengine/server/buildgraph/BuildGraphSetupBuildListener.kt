package com.jetbrains.teamcity.plugins.unrealengine.server.buildgraph

import arrow.core.Either
import arrow.core.raise.either
import com.jetbrains.teamcity.plugins.unrealengine.common.GenericError
import com.jetbrains.teamcity.plugins.unrealengine.common.UnrealPluginLoggers
import com.jetbrains.teamcity.plugins.unrealengine.server.extensions.logError
import jetbrains.buildServer.serverSide.BuildServerAdapter
import jetbrains.buildServer.serverSide.SRunningBuild

class BuildGraphSetupBuildListener(
    private val orchestrator: BuildGraphDistributedSetupOrchestrator,
) : BuildServerAdapter() {
    companion object {
        private val logger = UnrealPluginLoggers.get<BuildGraphSetupBuildListener>()
    }

    override fun beforeBuildFinish(runningBuild: SRunningBuild) =
        when (val result = either { orchestrator.setupDistributedBuild(runningBuild) }) {
            is Either.Left ->
                when (val error = result.value) {
                    is BuildSkipped -> logger.debug("Skipping build. Details: ${error.message}")
                    is GenericError -> {
                        logger.logError(error, "An error occurred processing finishing build graph setup build: ")
                        error.exception?.let { throw it }
                        Unit
                    }
                    else -> logger.error("An unexpected error occurred processing finishing build graph setup build")
                }
            is Either.Right -> logger.debug("Successfully processed finishing build graph setup build ${runningBuild.fullName}")
        }
}
