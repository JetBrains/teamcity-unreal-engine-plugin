package com.jetbrains.teamcity.plugins.unrealengine.server.buildgraph

import arrow.core.raise.Raise
import com.jetbrains.teamcity.plugins.unrealengine.common.Error
import com.jetbrains.teamcity.plugins.unrealengine.server.build.state.DistributedBuildStateTracker
import com.jetbrains.teamcity.plugins.unrealengine.server.extensions.asBuildPromotionEx
import com.jetbrains.teamcity.plugins.unrealengine.server.extensions.asTriggeredBy
import jetbrains.buildServer.serverSide.BuildQueueEx
import jetbrains.buildServer.serverSide.SRunningBuild

class BuildGraphDistributedSetupOrchestrator(
    private val validator: BuildGraphSetupBuildValidator,
    private val definitionLoader: BuildGraphDefinitionLoader,
    private val dependencyConnector: BuildGraphDependencyConnector,
    private val buildCreator: BuildGraphDistributedBuildCreator,
    private val buildStateTracker: DistributedBuildStateTracker,
    private val settingsInitializer: BuildGraphSettingsInitializer,
    private val buildQueue: BuildQueueEx,
) {
    context(_: Raise<Error>)
    fun setupDistributedBuild(setupBuild: SRunningBuild) {
        val (validatedSetupBuild, originalBuild) = validator.validate(setupBuild)
        val buildGraph = definitionLoader.loadFrom(validatedSetupBuild)

        val settings = settingsInitializer.initializeBuildSettings(originalBuild, buildGraph.badges)
        val distributedBuild =
            buildCreator.create(originalBuild, buildGraph).also {
                it.builds.onEach { build -> build.persist() }
            }

        if (settings.badgePosting is BadgePostingConfig.Enabled) {
            buildStateTracker.track(originalBuild, distributedBuild)
        }

        dependencyConnector.connect(validatedSetupBuild, distributedBuild, originalBuild)

        buildQueue.addToQueue(
            distributedBuild.builds.associateWith { null },
            originalBuild.buildPromotion.asBuildPromotionEx().asTriggeredBy(),
        )
    }
}
