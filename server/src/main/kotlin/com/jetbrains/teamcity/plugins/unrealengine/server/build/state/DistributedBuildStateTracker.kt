package com.jetbrains.teamcity.plugins.unrealengine.server.build.state

import arrow.core.raise.Raise
import com.jetbrains.teamcity.plugins.unrealengine.common.Error
import com.jetbrains.teamcity.plugins.unrealengine.common.UnrealPluginLoggers
import com.jetbrains.teamcity.plugins.unrealengine.common.build.events.RunnerInternalParameters
import com.jetbrains.teamcity.plugins.unrealengine.common.build.events.StepOutcome
import com.jetbrains.teamcity.plugins.unrealengine.common.ensureNotNull
import com.jetbrains.teamcity.plugins.unrealengine.server.build.DistributedBuild
import com.jetbrains.teamcity.plugins.unrealengine.server.build.state.DistributedBuildEvent.*
import com.jetbrains.teamcity.plugins.unrealengine.server.build.state.DistributedBuildState.BuildStep
import com.jetbrains.teamcity.plugins.unrealengine.server.build.state.DistributedBuildState.BuildStepState
import com.jetbrains.teamcity.plugins.unrealengine.server.extensions.activeRunners
import com.jetbrains.teamcity.plugins.unrealengine.server.extensions.asBuildPromotionEx
import com.jetbrains.teamcity.plugins.unrealengine.server.extensions.getGeneratedById
import jetbrains.buildServer.serverSide.BuildPromotionManager
import jetbrains.buildServer.serverSide.SBuild

class DistributedBuildStateTracker(
    private val promotionManager: BuildPromotionManager,
    private val stateStorage: DistributedBuildStateStorage,
    private val eventBus: DistributedBuildStateChangedEventBus,
) {
    companion object {
        private val logger = UnrealPluginLoggers.get<DistributedBuildStateTracker>()
    }

    fun track(
        originalBuild: SBuild,
        distributedBuild: DistributedBuild,
    ) {
        distributedBuild.builds.forEach {
            it.activeRunners().forEach { runner ->
                it.buildType!!.settings.updateBuildRunner(
                    runner.id,
                    runner.name,
                    runner.type,
                    buildMap {
                        putAll(runner.parameters)
                        notifyServerAboutExecution()
                    },
                )
            }
        }

        val initialState =
            DistributedBuildState(
                distributedBuild.builds.map { build ->
                    DistributedBuildState.Build(
                        build.buildType!!.name,
                        build.activeRunners().map { step ->
                            BuildStep(
                                step.name,
                                DistributedBuildState.BuildStepState.Pending,
                            )
                        },
                    )
                },
            )

        stateStorage.init(originalBuild, initialState)
    }

    private fun MutableMap<String, String>.notifyServerAboutExecution() =
        put(RunnerInternalParameters.BUILD_STEP_NOTIFICATIONS_ENABLED, true.toString())

    context(_: Raise<Error>)
    suspend fun handleBuildEvent(event: DistributedBuildEvent) {
        val parentBuild = promotionManager.findParentBuild(event.build) ?: return

        val updatedState =
            when (event) {
                is BuildStepStarted -> buildStepStarted(parentBuild, event)
                is BuildStepCompleted -> buildStepFinished(parentBuild, event)
                is BuildStepInterrupted -> buildStepInterrupted(parentBuild, event)
                is BuildSkipped -> buildSkipped(parentBuild, event.build)
            }

        if (buildCompleted(updatedState)) {
            stateStorage.dispose(parentBuild)
        }
    }

    context(_: Raise<Error>)
    private suspend fun buildStepStarted(
        parentBuild: SBuild,
        event: BuildStepStarted,
    ): DistributedBuildState {
        val updatedState =
            stateStorage.update(
                parentBuild,
                sequenceOf(BuildStep(event.name, BuildStepState.Running)),
            )

        eventBus.dispatch(
            DistributedBuildStateChanged.BuildStepStarted(
                parentBuild.buildId,
                updatedState,
                event.name,
            ),
        )

        return updatedState
    }

    context(_: Raise<Error>)
    private suspend fun buildStepFinished(
        parentBuild: SBuild,
        event: BuildStepCompleted,
    ): DistributedBuildState {
        // assume all steps after the failed one are skipped
        val skippedSteps =
            if (event.outcome == StepOutcome.Failure) {
                stateStorage
                    .get(parentBuild)
                    .findBuild(event.build.buildTypeName)
                    .getAllStepsAfter(event.name)
                    .asSkipped()
            } else {
                emptySequence()
            }

        val updatedState =
            stateStorage.update(
                parentBuild,
                sequenceOf(BuildStep(event.name, BuildStepState.Completed, event.outcome)) + skippedSteps,
            )

        eventBus.dispatch(
            DistributedBuildStateChanged.BuildStepCompleted(
                parentBuild.buildId,
                updatedState,
                event.name,
                event.outcome,
            ),
        )

        return updatedState
    }

    context(_: Raise<Error>)
    private suspend fun buildStepInterrupted(
        parentBuild: SBuild,
        event: BuildStepInterrupted,
    ): DistributedBuildState {
        val skippedSteps =
            stateStorage
                .get(parentBuild)
                .findBuild(event.build.buildTypeName)
                .getAllStepsAfter(event.name)
                .asSkipped()

        val updatedState =
            stateStorage.update(
                parentBuild,
                sequenceOf(BuildStep(event.name, BuildStepState.Interrupted)) + skippedSteps,
            )

        eventBus.dispatch(
            DistributedBuildStateChanged.BuildStepInterrupted(
                parentBuild.buildId,
                updatedState,
                event.build.buildTypeName,
                event.name,
            ),
        )

        return updatedState
    }

    context(_: Raise<Error>)
    private suspend fun buildSkipped(
        parentBuild: SBuild,
        eventBuild: SBuild,
    ): DistributedBuildState {
        val skippedSteps =
            stateStorage
                .get(parentBuild)
                .findBuild(eventBuild.buildTypeName)
                .steps
                .asSequence()
                .asSkipped()

        val updatedState = stateStorage.update(parentBuild, skippedSteps)

        eventBus.dispatch(
            DistributedBuildStateChanged.BuildSkipped(
                parentBuild.buildId,
                updatedState,
                eventBuild.buildTypeName,
            ),
        )

        return updatedState
    }

    context(_: Raise<Error>)
    private fun DistributedBuildState.findBuild(name: String): DistributedBuildState.Build =
        builds
            .firstOrNull { it.name == name }
            .let {
                ensureNotNull(
                    it,
                    "Unable to find a corresponding build in the distributed build state of the parent build",
                )
            }

    private fun DistributedBuildState.Build.getAllStepsAfter(stepName: String) =
        steps
            .asSequence()
            .dropWhile { it.name != stepName }
            .drop(1)

    private fun Sequence<BuildStep>.asSkipped() = map { BuildStep(it.name, BuildStepState.Skipped) }

    private fun BuildPromotionManager.findParentBuild(build: SBuild): SBuild? {
        val parentBuild =
            build.buildPromotion.asBuildPromotionEx().getGeneratedById()?.let {
                findPromotionById(it)
            }

        if (parentBuild == null) {
            logger.debug(
                "Unable to find the parent promotion for the build. It seems that it is not part of a generated distributed build, skipping",
            )
            return null
        }

        val associatedBuild = parentBuild.associatedBuild
        if (associatedBuild == null) {
            logger.debug("Parent promotion has no associated build, skipping")
            return null
        }

        return associatedBuild
    }

    private fun buildCompleted(state: DistributedBuildState): Boolean =
        state.builds
            .all { build ->
                build.steps.all {
                    it.state == BuildStepState.Completed || it.state == BuildStepState.Skipped || it.state == BuildStepState.Interrupted
                }
            }
}
