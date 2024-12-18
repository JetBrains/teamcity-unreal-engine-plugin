package com.jetbrains.teamcity.plugins.unrealengine.server.runner

import com.jetbrains.teamcity.plugins.unrealengine.common.automation.tests.AutomationTestsExecCommandParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.automation.tests.AutomationTestsFilterParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.automation.tests.AutomationTestsParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.automation.tests.AutomationTestsProjectPathParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.automation.tests.NullRHIParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun.ArchiveSwitchParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun.BuildConfigurationParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun.BuildCookRunProjectPathParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun.CompressedContentParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun.CookStageSwitchParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun.PackageStageSwitchParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun.PrerequisitesParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun.StageStageSwitchParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun.UnrealTargetConfigurationsParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun.UnversionedCookedContentParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun.UsePakParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.buildgraph.BuildGraphOptionsParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.buildgraph.BuildGraphScriptPathParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.buildgraph.BuildGraphTargetNodeParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.parameters.AdditionalArgumentsParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.parameters.EngineDetectionModeParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.parameters.UnrealCommandTypeParameter

class UnrealEngineRunnerParametersProvider {
    fun getDefaultValues() =
        (
            generalParameters +
                buildCookRunParameters +
                buildGraphParameters +
                runAutomationTestsParameters
        ).associate { it.name to it.defaultValue }

    private val generalParameters
        get() =
            sequenceOf(
                EngineDetectionModeParameter,
                UnrealCommandTypeParameter,
                AdditionalArgumentsParameter,
            )

    private val buildCookRunParameters
        get() =
            sequenceOf(
                BuildConfigurationParameter,
                UnrealTargetConfigurationsParameter.Standalone,
                UnrealTargetConfigurationsParameter.Client,
                UnrealTargetConfigurationsParameter.Server,
                CookStageSwitchParameter,
                UnversionedCookedContentParameter,
                StageStageSwitchParameter,
                UsePakParameter,
                CompressedContentParameter,
                PrerequisitesParameter,
                PackageStageSwitchParameter,
                ArchiveSwitchParameter,
                BuildCookRunProjectPathParameter,
            )

    private val buildGraphParameters
        get() =
            sequenceOf(
                BuildGraphScriptPathParameter,
                BuildGraphTargetNodeParameter,
                BuildGraphOptionsParameter,
            )

    private val runAutomationTestsParameters
        get() =
            sequenceOf(
                NullRHIParameter,
                AutomationTestsExecCommandParameter,
                AutomationTestsFilterParameter,
                AutomationTestsParameter,
                AutomationTestsProjectPathParameter,
            )
}
