package com.jetbrains.teamcity.plugins.unrealengine.server.build.state

import com.jetbrains.teamcity.plugins.unrealengine.common.build.events.StepOutcome
import jetbrains.buildServer.serverSide.SBuild

sealed interface DistributedBuildEvent {
    val build: SBuild

    data class BuildStepInterrupted(
        override val build: SBuild,
        val name: String,
    ) : DistributedBuildEvent

    data class BuildStepStarted(
        override val build: SBuild,
        val name: String,
    ) : DistributedBuildEvent

    data class BuildStepCompleted(
        override val build: SBuild,
        val name: String,
        val outcome: StepOutcome,
    ) : DistributedBuildEvent

    data class BuildSkipped(
        override val build: SBuild,
    ) : DistributedBuildEvent
}
