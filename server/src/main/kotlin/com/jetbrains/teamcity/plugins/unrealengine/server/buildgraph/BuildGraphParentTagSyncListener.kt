package com.jetbrains.teamcity.plugins.unrealengine.server.buildgraph

import com.jetbrains.teamcity.plugins.unrealengine.common.UnrealPluginLoggers
import com.jetbrains.teamcity.plugins.unrealengine.server.extensions.asBuildPromotionEx
import com.jetbrains.teamcity.plugins.unrealengine.server.extensions.getGeneratedById
import jetbrains.buildServer.serverSide.BuildPromotionManager
import jetbrains.buildServer.serverSide.BuildServerAdapter
import jetbrains.buildServer.serverSide.SBuild
import jetbrains.buildServer.serverSide.SRunningBuild

class BuildGraphParentTagSyncListener(
    private val promotionManager: BuildPromotionManager,
) : BuildServerAdapter() {
    companion object {
        private val logger = UnrealPluginLoggers.get<BuildGraphParentTagSyncListener>()
    }

    override fun beforeBuildFinish(runningBuild: SRunningBuild) {
        val parentBuild = findParentBuild(runningBuild) ?: return
        val childTags = runningBuild.tags
        if (childTags.isEmpty()) {
            return
        }

        val mergedTags = (parentBuild.tags + childTags).distinct()
        if (mergedTags == parentBuild.tags) {
            return
        }

        logger.debug(
            "Syncing tags from generated BuildGraph build ${runningBuild.buildId} to parent build ${parentBuild.buildId}: $childTags",
        )
        parentBuild.setTags(mergedTags)
    }

    private fun findParentBuild(build: SBuild): SBuild? {
        val generatedById = build.buildPromotion.asBuildPromotionEx().getGeneratedById() ?: return null
        val parentPromotion = promotionManager.findPromotionById(generatedById) ?: return null

        return parentPromotion.associatedBuild
    }
}

