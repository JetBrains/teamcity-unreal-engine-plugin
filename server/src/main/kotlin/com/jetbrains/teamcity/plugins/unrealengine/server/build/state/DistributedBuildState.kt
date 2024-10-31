package com.jetbrains.teamcity.plugins.unrealengine.server.build.state

import com.jetbrains.teamcity.plugins.unrealengine.common.build.events.StepOutcome
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DistributedBuildState(
    val builds: Collection<Build>,
) {
    @Serializable
    data class Build(
        val name: String,
        val steps: Collection<BuildStep>,
    )

    @Serializable
    data class BuildStep(
        val name: String,
        val state: BuildStepState,
        val outcome: StepOutcome? = null,
    )

    @Serializable
    enum class BuildStepState {
        @SerialName("pending")
        Pending,

        @SerialName("running")
        Running,

        @SerialName("completed")
        Completed,

        @SerialName("interrupted")
        Interrupted,

        @SerialName("skipped")
        Skipped,
    }
}
