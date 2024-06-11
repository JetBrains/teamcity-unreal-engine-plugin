package com.jetbrains.teamcity.plugins.unrealengine.server.runner

import arrow.core.raise.either
import com.jetbrains.teamcity.plugins.unrealengine.common.EngineDetectionMode
import com.jetbrains.teamcity.plugins.unrealengine.common.UnrealEngineRunner
import com.jetbrains.teamcity.plugins.unrealengine.common.parameters.EngineDetectionModeParameter
import jetbrains.buildServer.requirements.Requirement
import jetbrains.buildServer.serverSide.RunType
import jetbrains.buildServer.web.openapi.PluginDescriptor

class UnrealEngineRunType(
    private val pluginDescriptor: PluginDescriptor,
    private val propertiesValidator: UnrealEngineRunnerPropertiesValidator,
    private val runnerParametersProvider: UnrealEngineRunnerParametersProvider,
    private val runnerDescriptionGenerator: RunnerDescriptionGenerator,
) : RunType() {
    override fun getRunnerPropertiesProcessor() = propertiesValidator

    override fun getEditRunnerParamsJspFilePath() = pluginDescriptor.getPluginResourcesPath("editRunnerProperties.jsp")

    override fun getViewRunnerParamsJspFilePath() = pluginDescriptor.getPluginResourcesPath("viewRunnerProperties.jsp")

    override fun getDefaultRunnerProperties() = runnerParametersProvider.getDefaultValues()

    override fun describeParameters(parameters: MutableMap<String, String>) = runnerDescriptionGenerator.generate(parameters)

    override fun getType() = UnrealEngineRunner.RUN_TYPE

    override fun getDisplayName() = "Unreal Engine"

    override fun getDescription() = "Provides build facilities for Unreal Engine projects"

    override fun getRunnerSpecificRequirements(runParameters: MutableMap<String, String>): MutableList<Requirement> {
        val mode = either { EngineDetectionModeParameter.parseDetectionMode(runParameters) }.getOrNull() ?: return mutableListOf()

        return when (mode) {
            is EngineDetectionMode.Automatic ->
                mutableListOf(
                    Requirements.engineExists(mode.identifier),
                )
            is EngineDetectionMode.Manual -> mutableListOf()
        }
    }

    override fun getIconUrl() = pluginDescriptor.getPluginResourcesPath("ue_logo.svg")
}
