package com.jetbrains.teamcity.plugins.unrealengine.server.buildgraph

import arrow.core.Either
import arrow.core.raise.Raise
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import com.jetbrains.teamcity.plugins.framework.common.TeamCityLoggers
import com.jetbrains.teamcity.plugins.unrealengine.common.Error
import com.jetbrains.teamcity.plugins.unrealengine.common.GenericError
import com.jetbrains.teamcity.plugins.unrealengine.common.buildgraph.BuildGraphMode
import com.jetbrains.teamcity.plugins.unrealengine.common.buildgraph.BuildGraphModeParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.buildgraph.BuildGraphSettings
import com.jetbrains.teamcity.plugins.unrealengine.common.ensureNotNull
import com.jetbrains.teamcity.plugins.unrealengine.server.build.state.DistributedBuildStateTracker
import com.jetbrains.teamcity.plugins.unrealengine.server.extensions.activeRunners
import com.jetbrains.teamcity.plugins.unrealengine.server.extensions.addDependencies
import com.jetbrains.teamcity.plugins.unrealengine.server.extensions.asBuildPromotionEx
import com.jetbrains.teamcity.plugins.unrealengine.server.extensions.asTriggeredBy
import com.jetbrains.teamcity.plugins.unrealengine.server.extensions.default
import com.jetbrains.teamcity.plugins.unrealengine.server.extensions.hasSingleDistributedBuildGraphStep
import com.jetbrains.teamcity.plugins.unrealengine.server.extensions.logError
import jetbrains.buildServer.serverSide.BuildPromotionEx
import jetbrains.buildServer.serverSide.BuildQueueEx
import jetbrains.buildServer.serverSide.BuildServerAdapter
import jetbrains.buildServer.serverSide.SAgentRestrictor
import jetbrains.buildServer.serverSide.SBuild
import jetbrains.buildServer.serverSide.SRunningBuild
import jetbrains.buildServer.serverSide.artifacts.BuildArtifact
import jetbrains.buildServer.serverSide.artifacts.BuildArtifacts
import jetbrains.buildServer.serverSide.artifacts.BuildArtifactsViewMode
import jetbrains.buildServer.serverSide.dependency.DependencyOptions
import jetbrains.buildServer.util.DependencyOptionSupportImpl

class BuildGraphSetupBuildListener(
    private val buildGraphParser: BuildGraphParser,
    private val settings: BuildGraphSettings,
    private val distributedBuildCreator: BuildGraphDistributedBuildCreator,
    private val distributedBuildStateTracker: DistributedBuildStateTracker,
    private val buildQueue: BuildQueueEx,
) : BuildServerAdapter() {
    companion object {
        private val logger = TeamCityLoggers.server<BuildGraphSetupBuildListener>()
    }

    override fun beforeBuildFinish(runningBuild: SRunningBuild) =
        when (val result = either { handleFinishingBuild(runningBuild) }) {
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

    @JvmInline
    private value class BuildSkipped(
        val message: String,
    ) : Error

    context(Raise<Error>)
    private fun handleFinishingBuild(setupBuild: SRunningBuild) {
        val buildType =
            ensureNotNull(setupBuild.buildType) {
                BuildSkipped("The running build \"${setupBuild.fullName}\" is missing a build type")
            }

        val isBuildGraphSetup =
            buildType.isVirtual &&
                setupBuild.buildPromotion.hasSingleDistributedBuildGraphStep() &&
                buildType.name == settings.setupBuildName

        ensure(isBuildGraphSetup) {
            BuildSkipped("The running build \"${setupBuild.fullName}\" isn't a build graph setup build")
        }

        ensureNotNull(setupBuild.projectId) {
            BuildSkipped("Build graph setup build is missing project id. Distributed build won't be created")
        }

        val buildGraphFile =
            ensureNotNull(
                setupBuild.findPublishedGraph(),
                "It appears that the build graph setup build has failed, it hasn't published the exported graph file",
            )

        logger.debug("Found published build graph file for build \"${setupBuild.fullName}\"")
        val buildGraph = buildGraphParser.parse(buildGraphFile.inputStream)
        setupDistributedBuild(setupBuild, buildGraph)
    }

    private fun SRunningBuild.findPublishedGraph(): BuildArtifact? {
        var result: BuildArtifact? = null

        getArtifacts(BuildArtifactsViewMode.VIEW_DEFAULT).iterateArtifacts {
            if (it.name == settings.graphArtifactName && it.isFile) {
                result = it
                BuildArtifacts.BuildArtifactsProcessor.Continuation.BREAK
            } else {
                BuildArtifacts.BuildArtifactsProcessor.Continuation.CONTINUE
            }
        }

        return result
    }

    context(Raise<Error>)
    private fun setupDistributedBuild(
        setupBuild: SRunningBuild,
        buildGraph: BuildGraph<BuildGraphNodeGroup>,
    ) {
        val setupBuildPromotion = setupBuild.buildPromotion.asBuildPromotionEx()
        val originalBuild =
            ensureNotNull(
                setupBuildPromotion.dependedOnMe
                    .single()
                    .dependent.associatedBuild,
                "Unable to find the original build for ${setupBuild.fullName}",
            )
        val originalBuildPromotion = originalBuild.buildPromotion.asBuildPromotionEx()

        val settings = originalBuild.initBuildWideSettings(buildGraph.badges)
        val build =
            distributedBuildCreator
                .create(originalBuild, buildGraph)
                .also {
                    if (settings.badgePosting is BadgePostingConfig.Enabled) {
                        distributedBuildStateTracker.track(originalBuild, it)
                    }
                }

        setupBuildPromotion.addAsADependencyTo(build.starts)
        originalBuildPromotion.addDependencies(build.ends) {
            setOption(DependencyOptions.RUN_BUILD_IF_DEPENDENCY_FAILED, DependencyOptions.BuildContinuationMode.RUN_ADD_PROBLEM)
            setOption(DependencyOptions.RUN_BUILD_IF_DEPENDENCY_FAILED_TO_START, DependencyOptions.BuildContinuationMode.RUN_ADD_PROBLEM)
        }
        originalBuildPromotion.removeDependency(setupBuildPromotion)

        buildQueue.addToQueue(build.builds.toMapWithoutAgentRestrictions(), originalBuildPromotion.asTriggeredBy())

        originalBuildPromotion.persist()
        setupBuildPromotion.persist()
    }

    context(Raise<Error>)
    private fun SBuild.initBuildWideSettings(badges: Collection<Badge>): BuildGraphBuildSettings {
        val originalRunnerParameters =
            buildPromotion
                .activeRunners()
                .single()
                .parameters

        val modeSettings = BuildGraphModeParameter.parse(originalRunnerParameters) as? BuildGraphMode.Distributed

        val badgePostingConfig =
            if (modeSettings?.metadataServerUrl == null) {
                BadgePostingConfig.Disabled
            } else {
                BadgePostingConfig.Enabled(modeSettings.metadataServerUrl!!, badges)
            }

        val settings = BuildGraphBuildSettings(badgePostingConfig)
        addBuildGraphBuildSettings(settings)
        return settings
    }

    private fun BuildPromotionEx.addAsADependencyTo(dependents: Collection<BuildPromotionEx>) {
        val dependencyOptions = DependencyOptionSupportImpl().default()
        for (dependent in dependents) {
            dependent.addDependency(this, dependencyOptions)
        }
    }

    private fun List<BuildPromotionEx>.toMapWithoutAgentRestrictions(): Map<BuildPromotionEx, SAgentRestrictor?> = associateWith { null }
}
