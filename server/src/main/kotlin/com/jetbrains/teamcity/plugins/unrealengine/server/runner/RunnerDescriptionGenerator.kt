package com.jetbrains.teamcity.plugins.unrealengine.server.runner

import arrow.core.getOrElse
import arrow.core.raise.either
import com.jetbrains.teamcity.plugins.unrealengine.common.EngineDetectionMode
import com.jetbrains.teamcity.plugins.unrealengine.common.UnrealCommandType
import com.jetbrains.teamcity.plugins.unrealengine.common.UnrealPluginLoggers
import com.jetbrains.teamcity.plugins.unrealengine.common.automation.commands.AutomationCommandNameParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.automation.tests.AutomationTestsProjectPathParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun.BuildCookRunProjectPathParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.buildgraph.BuildGraphModeParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.buildgraph.BuildGraphScriptPathParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.buildgraph.BuildGraphTargetNodeParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.commandlets.CommandletArgumentsParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.commandlets.CommandletNameParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.commandlets.CommandletProjectPathParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.commandlets.EditorExecutableParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.parameters.EngineDetectionModeParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.parameters.SelectParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.parameters.UnrealCommandTypeParameter

class RunnerDescriptionGenerator {
    companion object {
        private val logger = UnrealPluginLoggers.get<RunnerDescriptionGenerator>()
    }

    fun generate(runnerParameters: Map<String, String>): String =
        either {
            val commandParameters =
                when (UnrealCommandTypeParameter.parse(runnerParameters)) {
                    UnrealCommandType.BuildCookRun ->
                        sequenceOf(
                            UnrealCommandTypeParameter,
                            BuildCookRunProjectPathParameter,
                        )
                    UnrealCommandType.BuildGraph ->
                        sequenceOf(
                            UnrealCommandTypeParameter,
                            BuildGraphScriptPathParameter,
                            BuildGraphTargetNodeParameter,
                            BuildGraphModeParameter,
                        )
                    UnrealCommandType.RunAutomationTests ->
                        sequenceOf(
                            UnrealCommandTypeParameter,
                            AutomationTestsProjectPathParameter,
                        )
                    UnrealCommandType.RunAutomationCommand ->
                        sequenceOf(
                            UnrealCommandTypeParameter,
                            AutomationCommandNameParameter,
                        )
                    UnrealCommandType.RunCommandlet ->
                        sequenceOf(
                            UnrealCommandTypeParameter,
                            EditorExecutableParameter,
                            CommandletProjectPathParameter,
                            CommandletNameParameter,
                            CommandletArgumentsParameter,
                        )
                }.associate {
                    when (it) {
                        is SelectParameter -> it.displayName to it.getOptionDisplayName(runnerParameters[it.name])
                        else -> it.displayName to runnerParameters[it.name]
                    }
                }

            val detectionModeParameters =
                when (val detectionMode = EngineDetectionModeParameter.parseDetectionMode(runnerParameters)) {
                    is EngineDetectionMode.Automatic ->
                        mapOf(
                            "Engine detection mode" to "Auto",
                            "Engine identifier" to detectionMode.identifier.value,
                        )
                    is EngineDetectionMode.Manual ->
                        mapOf(
                            "Engine detection mode" to "Manual",
                            "Engine Root path" to detectionMode.engineRootPath.value,
                        )
                }

            (commandParameters + detectionModeParameters)
                .asSequence()
                .filter { it.value != null }
                .joinToString(separator = ", ") { "${it.key}: ${it.value}" }
        }.getOrElse {
            logger.info("There was an error during runner description generation: $it")

            ""
        }
}
