package com.jetbrains.teamcity.plugins.unrealengine.server.runner

import arrow.core.getOrElse
import arrow.core.raise.either
import com.jetbrains.teamcity.plugins.framework.common.TeamCityLoggers
import com.jetbrains.teamcity.plugins.unrealengine.common.EngineDetectionMode
import com.jetbrains.teamcity.plugins.unrealengine.common.UnrealCommandType
import com.jetbrains.teamcity.plugins.unrealengine.common.automation.AutomationProjectPathParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun.BuildCookRunProjectPathParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.buildgraph.BuildGraphModeParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.buildgraph.BuildGraphScriptPathParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.buildgraph.BuildGraphTargetNodeParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.parameters.EngineDetectionModeParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.parameters.UnrealCommandTypeParameter

class RunnerDescriptionGenerator {
    companion object {
        private val logger = TeamCityLoggers.agent<RunnerDescriptionGenerator>()
    }

    fun generate(runnerParameters: Map<String, String>): String = either {
        val commandParameters = when (UnrealCommandTypeParameter.parse(runnerParameters)) {
            UnrealCommandType.BuildCookRun -> sequenceOf(
                UnrealCommandTypeParameter,
                BuildCookRunProjectPathParameter,
            )
            UnrealCommandType.BuildGraph -> sequenceOf(
                UnrealCommandTypeParameter,
                BuildGraphScriptPathParameter,
                BuildGraphTargetNodeParameter,
                BuildGraphModeParameter,
            )
            UnrealCommandType.RunAutomation -> sequenceOf(
                UnrealCommandTypeParameter,
                AutomationProjectPathParameter,
            )
        }.associate { it.displayName to runnerParameters[it.name] }

        val detectionModeParameters = when (val detectionMode = EngineDetectionModeParameter.parseDetectionMode(runnerParameters)) {
            is EngineDetectionMode.Automatic -> mapOf(
                "Engine detection mode" to "Auto",
                "Engine identifier" to detectionMode.identifier.value,
            )
            is EngineDetectionMode.Manual -> mapOf(
                "Engine detection mode" to "Manual",
                "Engine Root path" to detectionMode.engineRootPath.value,
            )
        }

        (commandParameters + detectionModeParameters)
            .asSequence()
            .joinToString(separator = ", ") { "${it.key}: ${it.value}" }
    }.getOrElse {
        logger.info("There was an error during runner description generation: $it")

        ""
    }
}
