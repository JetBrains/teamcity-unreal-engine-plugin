package com.jetbrains.teamcity.plugins.unrealengine.agent

import arrow.core.raise.Raise
import arrow.core.raise.recover
import com.jetbrains.teamcity.plugins.unrealengine.common.CommandExecutionContext
import com.jetbrains.teamcity.plugins.unrealengine.common.EngineDetectionMode
import com.jetbrains.teamcity.plugins.unrealengine.common.GenericError
import com.jetbrains.teamcity.plugins.unrealengine.common.UnrealEngineIdentifier
import com.jetbrains.teamcity.plugins.unrealengine.common.UnrealEngineRootPath
import com.jetbrains.teamcity.plugins.unrealengine.common.UnrealEngineRunner
import com.jetbrains.teamcity.plugins.unrealengine.common.UnrealEngineVersion
import com.jetbrains.teamcity.plugins.unrealengine.common.UnrealPluginLoggers
import com.jetbrains.teamcity.plugins.unrealengine.common.ensure
import com.jetbrains.teamcity.plugins.unrealengine.common.parameters.EngineDetectionModeParameter.parseDetectionMode
import com.jetbrains.teamcity.plugins.unrealengine.common.raise
import jetbrains.buildServer.agent.BuildAgentConfiguration

data class UnrealEngine(
    val path: UnrealEngineRootPath,
    val version: UnrealEngineVersion,
)

class UnrealEngineProvider(
    private val agentConfiguration: BuildAgentConfiguration,
    private val engineVersionDetector: UnrealEngineSourceVersionDetector,
) {
    companion object {
        private val logger = UnrealPluginLoggers.get<UnrealEngineProvider>()
    }

    context(Raise<GenericError>, CommandExecutionContext)
    suspend fun findEngine(runnerParameters: Map<String, String>): UnrealEngine {
        val mode = recover({ parseDetectionMode(runnerParameters) }) { raise(it.message) }

        val rootPath =
            when (mode) {
                is EngineDetectionMode.Automatic -> {
                    findAmongAgentInstalledEngines(mode.identifier)
                }
                is EngineDetectionMode.Manual -> {
                    UnrealEngineRootPath(resolveUserPath(mode.engineRootPath.value))
                }
            }

        val engineVersion = engineVersionDetector.detect(rootPath)

        return UnrealEngine(rootPath, engineVersion)
    }

    context(Raise<GenericError>)
    private fun findAmongAgentInstalledEngines(engineIdentifier: UnrealEngineIdentifier): UnrealEngineRootPath {
        val matchingEngineIdentifiers =
            agentConfiguration.configurationParameters.keys
                .filter { it.isUnrealEnginePathParameter(engineIdentifier) }
                .map { it.extractFullEngineIdentifier() }
                .sortedDescending()

        ensure(matchingEngineIdentifiers.isNotEmpty(), "Specified identifier was not found among agent installed engines")

        if (matchingEngineIdentifiers.size > 1) {
            logger.info(
                "Multiple engines matching '${engineIdentifier.value}' were found. " +
                    "Will take the latest one based on a case-sensitive lexicographical sort order",
            )
        }

        val chosenIdentifier = matchingEngineIdentifiers.first()
        val location = agentConfiguration.configurationParameters[chosenIdentifier.toUnrealEnginePathParameter()]!!

        logger.info("Engine $chosenIdentifier located at $location was chosen for the build")

        return UnrealEngineRootPath(location)
    }

    private fun String.isUnrealEnginePathParameter(identifier: UnrealEngineIdentifier) =
        startsWith("${UnrealEngineRunner.AGENT_PARAMETER_NAME_PREFIX}.${identifier.value}") && endsWith(".path")

    private fun String.toUnrealEnginePathParameter() = "${UnrealEngineRunner.AGENT_PARAMETER_NAME_PREFIX}.$this.path"

    private fun String.extractFullEngineIdentifier() =
        substring(
            indexOfFirst { char -> char == '.' } + 1,
            indexOfLast { char -> char == '.' },
        )
}
