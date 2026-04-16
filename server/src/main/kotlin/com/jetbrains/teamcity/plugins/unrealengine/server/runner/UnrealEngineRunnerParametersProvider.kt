package com.jetbrains.teamcity.plugins.unrealengine.server.runner

import com.jetbrains.teamcity.plugins.unrealengine.common.automation.tests.AutomationTestsExecCommandParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.automation.tests.AutomationTestsFilterParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.automation.tests.AutomationTestsParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.automation.tests.AutomationTestsProjectPathParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.automation.tests.NullRHIParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun.ArchiveSwitchParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun.BuildConfigurationParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun.BuildCookRunProjectPathParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun.UnrealBuildTargetParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun.CrashReporterParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun.CompileParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun.CompressedContentParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun.CookStageSwitchParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun.CookAllParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun.CookMapsOnlyParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun.CookPartialGCParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun.AdditionalCookerOptionsParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun.DistributionParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun.ExcludeEditorContentParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun.FastCookParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun.GenerateChunksParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun.IgnoreCookErrorsParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun.InstalledBuildParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun.IterativeCookingParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun.LogWindowParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun.ManifestsParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun.NoDebugInfoParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun.NoCompileUATParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun.NoXGEParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun.PackageStageSwitchParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun.PrerequisitesParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun.SkipCookingEditorContentParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun.SkipEncryptionParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun.StageStageSwitchParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun.UnrealTargetConfigurationsParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun.UnversionedCookedContentParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun.UseIoStoreParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun.UsePakParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun.VerboseLoggingParameter
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
                UnrealBuildTargetParameter,
                CookStageSwitchParameter,
                UnversionedCookedContentParameter,
                GenerateChunksParameter,
                IterativeCookingParameter,
                CookAllParameter,
                CookMapsOnlyParameter,
                CookPartialGCParameter,
                FastCookParameter,
                IgnoreCookErrorsParameter,
                SkipCookingEditorContentParameter,
                ExcludeEditorContentParameter,
                AdditionalCookerOptionsParameter,
                StageStageSwitchParameter,
                UsePakParameter,
                UseIoStoreParameter,
                CompressedContentParameter,
                PrerequisitesParameter,
                PackageStageSwitchParameter,
                ArchiveSwitchParameter,
                CompileParameter,
                InstalledBuildParameter,
                NoCompileUATParameter,
                ManifestsParameter,
                CrashReporterParameter,
                DistributionParameter,
                VerboseLoggingParameter,
                LogWindowParameter,
                NoXGEParameter,
                NoDebugInfoParameter,
                SkipEncryptionParameter,
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
