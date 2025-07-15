package com.jetbrains.teamcity.plugins.unrealengine.server.buildgraph

import arrow.core.Either
import arrow.core.raise.Raise
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import com.jetbrains.teamcity.plugins.unrealengine.common.Error
import com.jetbrains.teamcity.plugins.unrealengine.common.GenericError
import com.jetbrains.teamcity.plugins.unrealengine.common.UnrealPluginLoggers
import com.jetbrains.teamcity.plugins.unrealengine.common.build.events.StepOutcome
import com.jetbrains.teamcity.plugins.unrealengine.common.buildgraph.BuildGraphSettings
import com.jetbrains.teamcity.plugins.unrealengine.server.build.state.DistributedBuildState
import com.jetbrains.teamcity.plugins.unrealengine.server.build.state.DistributedBuildState.BuildStepState
import com.jetbrains.teamcity.plugins.unrealengine.server.build.state.DistributedBuildStateChanged
import com.jetbrains.teamcity.plugins.unrealengine.server.build.state.DistributedBuildStateChangedEventHandler
import com.jetbrains.teamcity.plugins.unrealengine.server.build.status.ugs.BadgeState
import com.jetbrains.teamcity.plugins.unrealengine.server.build.status.ugs.PerforceDepotPath
import com.jetbrains.teamcity.plugins.unrealengine.server.build.status.ugs.UgsBuildMetadata
import com.jetbrains.teamcity.plugins.unrealengine.server.build.status.ugs.UgsMetadataServerClient
import com.jetbrains.teamcity.plugins.unrealengine.server.extensions.asBuildPromotionEx
import com.jetbrains.teamcity.plugins.unrealengine.server.extensions.getPerforceChangelistNumber
import com.jetbrains.teamcity.plugins.unrealengine.server.extensions.logError
import jetbrains.buildServer.serverSide.BuildsManager
import jetbrains.buildServer.serverSide.RelativeWebLinks
import jetbrains.buildServer.serverSide.SBuild
import jetbrains.buildServer.swarm.SwarmClientManager

class BuildGraphBadgePublisher(
    private val buildGraphSettings: BuildGraphSettings,
    private val buildsManager: BuildsManager,
    private val links: RelativeWebLinks,
    private val ugsMetadataServerClient: UgsMetadataServerClient,
) : DistributedBuildStateChangedEventHandler {
    companion object {
        private val logger = UnrealPluginLoggers.get<BuildGraphBadgePublisher>()
    }

    override suspend fun consume(event: DistributedBuildStateChanged) {
        when (val result = either { handleStateChange(event) }) {
            is Either.Left ->
                when (val error = result.value) {
                    is ProcessingSkipped ->
                        logger.debug("State update for the build ${event.buildId} skipped because: ${error.reason}")
                    is GenericError ->
                        logger.logError(error, "An unexpected error occurred processing state update for the build ${event.buildId}:")
                }
            is Either.Right -> {
                logger.debug("State update for the build ${event.buildId} processed successfully")
            }
        }
    }

    @JvmInline
    private value class ProcessingSkipped(
        val reason: String,
    ) : Error

    context(Raise<Error>)
    private suspend fun handleStateChange(event: DistributedBuildStateChanged) {
        val build =
            ensureNotNull(buildsManager.findBuildInstanceById(event.buildId)) {
                ProcessingSkipped("Unable to find build with the given id ${event.buildId}")
            }

        ensure(
            build.buildPromotion.asBuildPromotionEx().getAttribute(buildGraphSettings.buildGraphGeneratedMarker) != null,
        ) {
            ProcessingSkipped("Build isn't part of a distributed BuildGraph build")
        }

        val (ugsMetadataServerUrl, buildBadges) =
            when (val badgePostingConfig = build.getBuildGraphBuildSettings().badgePosting) {
                is BadgePostingConfig.Disabled -> {
                    raise(ProcessingSkipped("Badge posting is not enabled for this build"))
                }
                is BadgePostingConfig.Enabled -> (badgePostingConfig.ugsMetadataServerUrl to badgePostingConfig.badges.asSequence())
            }

        val badgesToPost =
            when (event) {
                is DistributedBuildStateChanged.BuildStepStarted -> findStartingBadges(buildBadges, event).map { it to BadgeState.Starting }
                is DistributedBuildStateChanged.BuildSkipped -> findSkippedBadges(buildBadges, event).map { it to BadgeState.Skipped }
                is DistributedBuildStateChanged.BuildStepCompleted ->
                    when (event.stepOutcome) {
                        StepOutcome.Success ->
                            findSucceededBadges(buildBadges, event).map { it to BadgeState.Success } +
                                findSkippedBadges(buildBadges, event).map { it to BadgeState.Skipped }
                        StepOutcome.Failure -> findFailedBadges(buildBadges, event).map { it to BadgeState.Failure }
                    }
                is DistributedBuildStateChanged.BuildStepInterrupted ->
                    findSkippedBadges(
                        buildBadges,
                        event,
                    ).map { it to BadgeState.Skipped }
            }

        val changeLists = build.getPerforceChangelists()
        for ((badge, state) in badgesToPost) {
            for (changeList in changeLists) {
                val metadata =
                    UgsBuildMetadata(
                        changeList,
                        PerforceDepotPath(badge.project),
                        badge.name,
                        links.getBuildDependenciesUrl(build),
                        state,
                    )
                ugsMetadataServerClient.postBuildMetadata(ugsMetadataServerUrl, metadata)
            }
        }
    }

    context(Raise<Error>)
    private fun SBuild.getPerforceChangelists(): List<Long> {
        val changeLists =
            revisions
                .asSequence()
                .filter { it.root.vcsName == SwarmClientManager.PERFORCE_VCS_NAME }
                .mapNotNull {
                    val changeList = it.getPerforceChangelistNumber()
                    if (changeList == null) {
                        logger.warn("Unable to parse changelist number from the revision \"${it.revision}\", it will be skipped")
                    }
                    changeList
                }.toList()

        ensure(changeLists.isNotEmpty()) {
            ProcessingSkipped("There are no valid Perforce revisions associated with this build")
        }

        if (changeLists.size > 1) {
            logger.info(
                "There are ${changeLists.size} Perforce revisions (CLs) associated with this build, badges will be posted for each of them",
            )
        }

        return changeLists
    }

    private fun findStartingBadges(
        buildBadges: Sequence<Badge>,
        event: DistributedBuildStateChanged.BuildStepStarted,
    ): Sequence<Badge> {
        val allSteps =
            event.buildState.builds
                .asSequence()
                .flatMap { it.steps }

        return buildBadges
            .findAffectedBadges(sequenceOf(event.stepName))
            .filter { badge ->
                allSteps
                    .filter { badge.nodes.contains(it.name) && it.name != event.stepName }
                    .all { it.state == BuildStepState.Pending }
            }
    }

    private fun findSucceededBadges(
        buildBadges: Sequence<Badge>,
        event: DistributedBuildStateChanged.BuildStepCompleted,
    ): Sequence<Badge> {
        val allSteps =
            event.buildState.builds
                .flatMap { it.steps }
                .asSequence()

        return buildBadges
            .findAffectedBadges(sequenceOf(event.stepName))
            .filter { badge ->
                allSteps
                    .filter { badge.nodes.contains(it.name) && it.name != event.stepName }
                    .all { it.state == BuildStepState.Completed && it.outcome == StepOutcome.Success }
            }
    }

    private fun findFailedBadges(
        buildBadges: Sequence<Badge>,
        event: DistributedBuildStateChanged.BuildStepCompleted,
    ): Sequence<Badge> {
        val allSteps =
            event.buildState.builds
                .asSequence()
                .flatMap { it.steps }

        return buildBadges
            .findAffectedBadges(sequenceOf(event.stepName))
            .filter { badge ->
                allSteps
                    .filter { badge.nodes.contains(it.name) && it.name != event.stepName }
                    .all { it.outcome != StepOutcome.Failure }
            }
    }

    private fun findSkippedBadges(
        buildBadges: Sequence<Badge>,
        event: DistributedBuildStateChanged.BuildSkipped,
    ): Sequence<Badge> {
        val skippedNodes =
            event.buildState
                .builds
                .first { it.name == event.buildName }
                .steps
                .asSequence()
                .map { it.name }

        return buildBadges
            .findAffectedBadges(skippedNodes)
            .findBadgesToSkip(event.buildState)
    }

    private fun findSkippedBadges(
        buildBadges: Sequence<Badge>,
        event: DistributedBuildStateChanged.BuildStepInterrupted,
    ): Sequence<Badge> {
        val skippedNodes =
            event.buildState.builds
                .first { it.name == event.buildName }
                .steps
                .asSequence()
                .dropWhile { it.name != event.stepName }
                .map { it.name }

        return buildBadges
            .findAffectedBadges(skippedNodes)
            .findBadgesToSkip(event.buildState)
    }

    private fun findSkippedBadges(
        buildBadges: Sequence<Badge>,
        event: DistributedBuildStateChanged.BuildStepCompleted,
    ): Sequence<Badge> {
        val stepsByName = event.buildState.stepsByName()
        val affectedBadges = buildBadges.findAffectedBadges(sequenceOf(event.stepName))

        return affectedBadges.filter { badge ->
            val buildSteps = badge.nodes.mapNotNull { stepsByName[it] }

            val otherSteps = buildSteps.filter { it.name != event.stepName }

            val hasSkipped = otherSteps.any { it.state == BuildStepState.Skipped || it.state == BuildStepState.Interrupted }
            val hasFailed = otherSteps.any { it.state == BuildStepState.Completed && it.outcome == StepOutcome.Failure }
            val hasRunning = otherSteps.any { it.state == BuildStepState.Running }

            hasSkipped && !hasRunning && !hasFailed
        }
    }

    private fun Sequence<Badge>.findAffectedBadges(changedNodes: Sequence<String>) =
        filter { it.nodes.any { node -> changedNodes.contains(node) } }

    private fun DistributedBuildState.stepsByName() = builds.asSequence().flatMap { it.steps }.associateBy { it.name }

    private fun Sequence<Badge>.findBadgesToSkip(state: DistributedBuildState): Sequence<Badge> {
        val stepsByName = state.stepsByName()

        return this
            .filter { badge ->
                badge.nodes
                    .mapNotNull { stepsByName[it] }
                    .all {
                        it.state == BuildStepState.Completed &&
                            it.outcome == StepOutcome.Success ||
                            it.state == BuildStepState.Skipped ||
                            it.state == BuildStepState.Interrupted
                    }
            }
    }
}
