package com.jetbrains.teamcity.plugins.unrealengine.server.build

import jetbrains.buildServer.serverSide.BuildPromotionEx

data class DistributedBuild(
    val builds: List<BuildPromotionEx>,
) {
    val starts by lazy { builds.filter { it.dependencies.isEmpty() } }
    val ends by lazy { builds.filter { it.dependedOnMe.isEmpty() } }
}
