package com.jetbrains.teamcity.plugins.unrealengine.server.build.state

import arrow.core.raise.either
import com.jetbrains.teamcity.plugins.unrealengine.common.UnrealPluginLoggers
import com.jetbrains.teamcity.plugins.unrealengine.server.extensions.getGeneratedById
import com.jetbrains.teamcity.plugins.unrealengine.server.extensions.isMainNode
import com.jetbrains.teamcity.plugins.unrealengine.server.extensions.logResult
import jetbrains.buildServer.messages.ErrorData
import jetbrains.buildServer.serverSide.BuildEx
import jetbrains.buildServer.serverSide.BuildPromotionEx
import jetbrains.buildServer.serverSide.BuildServerAdapter
import jetbrains.buildServer.serverSide.SFinishedBuild
import jetbrains.buildServer.serverSide.ServerResponsibility
import kotlinx.coroutines.runBlocking

class SkippedBuildMonitor(
    private val serverResponsibility: ServerResponsibility,
    private val stateTracker: DistributedBuildStateTracker,
) : BuildServerAdapter() {
    companion object {
        private val logger = UnrealPluginLoggers.get<SkippedBuildMonitor>()
    }

    override fun entryCreated(build: SFinishedBuild) =
        runBlocking {
            val buildId = build.buildId
            (build.buildPromotion as? BuildPromotionEx)?.getGeneratedById() ?: return@runBlocking

            if (!build.cancelled() || build.isStartedOnAgent) {
                logger.debug(
                    "Ignoring build \"$buildId\" event since it doesn't represent a cancelled build or because the build has already been started",
                )
                return@runBlocking
            }

            if (!serverResponsibility.isMainNode()) {
                logger.debug("Ignoring skipped build \"$buildId\" since this isn't the main node")
                return@runBlocking
            }

            either { stateTracker.handleBuildEvent(DistributedBuildEvent.BuildSkipped(build)) }.let {
                logger.logResult(it, "Skipped build tracking")
            }

            super.entryCreated(build)
        }

    private fun SFinishedBuild.cancelled() = cancelledManually() || hasFailedDependency()

    private fun SFinishedBuild.cancelledManually() = canceledInfo != null

    private fun SFinishedBuild.hasFailedDependency() =
        (this as BuildEx).buildProblems.any { problem ->
            problem.buildProblemData.type == ErrorData.SNAPSHOT_DEPENDENCY_ERROR_TYPE
        }
}
