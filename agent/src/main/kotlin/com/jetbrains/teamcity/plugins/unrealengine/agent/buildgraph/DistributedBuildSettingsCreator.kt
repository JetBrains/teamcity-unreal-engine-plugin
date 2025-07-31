package com.jetbrains.teamcity.plugins.unrealengine.agent.buildgraph

import arrow.core.raise.Raise
import com.jetbrains.teamcity.plugins.framework.common.ensureNotNull
import com.jetbrains.teamcity.plugins.framework.common.raise
import com.jetbrains.teamcity.plugins.unrealengine.common.UnrealPluginLoggers
import com.jetbrains.teamcity.plugins.unrealengine.common.buildgraph.BuildGraphRunnerInternalSettings
import jetbrains.buildServer.agent.BuildAgentConfiguration

sealed interface DistributedBuildSettings {
    data class SetupBuildSettings(
        val exportedGraphPath: String,
        val networkShare: String,
        val compositeBuildId: String,
    ) : DistributedBuildSettings

    data class RegularBuildSettings(
        val networkShare: String,
        val compositeBuildId: String,
    ) : DistributedBuildSettings
}

@JvmInline
value class SettingsCreationError(
    val message: String,
)

class DistributedBuildSettingsCreator(
    private val agentConfiguration: BuildAgentConfiguration,
) {
    companion object {
        private val logger = UnrealPluginLoggers.get<DistributedBuildSettingsCreator>()
    }

    context(_: Raise<SettingsCreationError>)
    fun from(runnerParameters: Map<String, String>): DistributedBuildSettings {
        val runnerInternalSettings =
            runCatching {
                BuildGraphRunnerInternalSettings.fromRunnerParameters(runnerParameters)
            }.getOrElse {
                logger.error("Runner internal settings are corrupted", it)
                raise(SettingsCreationError("Unable to retrieve runner internal settings. See the agent logs for details"))
            }

        val sharedDirAgentParameter = "unreal-engine.build-graph.agent.shared-dir"
        val sharedStorageDir = agentConfiguration.configurationParameters[sharedDirAgentParameter]
        ensureNotNull(sharedStorageDir) {
            val message =
                "BuildGraph shared storage directory parameter ($sharedDirAgentParameter) is not specified on the agent"
            logger.error(message)
            SettingsCreationError(message)
        }

        return when (runnerInternalSettings) {
            is BuildGraphRunnerInternalSettings.SetupBuildSettings -> {
                DistributedBuildSettings.SetupBuildSettings(
                    runnerInternalSettings.exportedGraphPath,
                    sharedStorageDir,
                    runnerInternalSettings.compositeBuildId,
                )
            }
            is BuildGraphRunnerInternalSettings.RegularBuildSettings -> {
                DistributedBuildSettings.RegularBuildSettings(
                    sharedStorageDir,
                    runnerInternalSettings.compositeBuildId,
                )
            }
        }
    }
}
