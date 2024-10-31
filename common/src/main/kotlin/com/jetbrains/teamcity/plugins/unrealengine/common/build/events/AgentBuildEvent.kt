package com.jetbrains.teamcity.plugins.unrealengine.common.build.events

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

sealed interface AgentBuildEvent {
    @Serializable
    @SerialName("build-step.started")
    data class BuildStepStarted(
        val name: String,
    ) : AgentBuildEvent

    @Serializable
    @SerialName("build-step.interrupted")
    data class BuildStepInterrupted(
        val name: String,
    ) : AgentBuildEvent

    @Serializable
    @SerialName("build-step.completed")
    data class BuildStepCompleted(
        val name: String,
        val outcome: StepOutcome,
    ) : AgentBuildEvent
}

@Serializable
@SerialName("outcome")
enum class StepOutcome {
    @SerialName("success")
    Success,

    @SerialName("failure")
    Failure,
}
