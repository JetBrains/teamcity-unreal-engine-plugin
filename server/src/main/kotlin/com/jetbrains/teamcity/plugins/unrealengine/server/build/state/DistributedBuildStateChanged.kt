package com.jetbrains.teamcity.plugins.unrealengine.server.build.state

import com.jetbrains.teamcity.plugins.unrealengine.common.build.events.StepOutcome

sealed interface DistributedBuildStateChanged {
    val buildId: Long
    val buildState: DistributedBuildState

    data class BuildStepCompleted(
        override val buildId: Long,
        override val buildState: DistributedBuildState,
        val stepName: String,
        val stepOutcome: StepOutcome,
    ) : DistributedBuildStateChanged

    data class BuildStepStarted(
        override val buildId: Long,
        override val buildState: DistributedBuildState,
        val stepName: String,
    ) : DistributedBuildStateChanged

    data class BuildStepInterrupted(
        override val buildId: Long,
        override val buildState: DistributedBuildState,
        val buildName: String,
        val stepName: String,
    ) : DistributedBuildStateChanged

    data class BuildSkipped(
        override val buildId: Long,
        override val buildState: DistributedBuildState,
        val buildName: String,
    ) : DistributedBuildStateChanged
}
