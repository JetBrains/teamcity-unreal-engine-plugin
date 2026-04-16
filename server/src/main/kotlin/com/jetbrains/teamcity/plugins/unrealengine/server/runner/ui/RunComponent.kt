package com.jetbrains.teamcity.plugins.unrealengine.server.runner.ui

import com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun.ArchiveDirectoryParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun.ArchiveSwitchParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun.CrashReporterParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun.CompileParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun.CompressedContentParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun.DistributionParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun.InstalledBuildParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun.LogWindowParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun.ManifestsParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun.NoDebugInfoParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun.NoCompileUATParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun.NoXGEParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun.PackageStageSwitchParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun.PrerequisitesParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun.SkipEncryptionParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun.StageStageSwitchParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun.StagingDirectoryParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun.UseIoStoreParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun.UsePakParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun.VerboseLoggingParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.parameters.CheckboxParameter

class RunComponent {
    val performStage = StageStageSwitchParameter
    val stagingDirectory = StagingDirectoryParameter
    val usePak = UsePakParameter
    val compressedContent = CompressedContentParameter
    val prerequisites = PrerequisitesParameter

    val performPackage = PackageStageSwitchParameter

    val archiveBuild = ArchiveSwitchParameter
    val archiveDirectory = ArchiveDirectoryParameter

    val compile = CompileParameter
    val installedBuild = InstalledBuildParameter
    val useIoStore = UseIoStoreParameter
    val noCompileUAT = NoCompileUATParameter
    val manifests = ManifestsParameter
    val crashReporter = CrashReporterParameter
    val distribution = DistributionParameter
    val verboseLogging = VerboseLoggingParameter
    val logWindow = LogWindowParameter
    val noXGE = NoXGEParameter
    val noDebugInfo = NoDebugInfoParameter
    val skipEncryption = SkipEncryptionParameter

    fun formatFlags(properties: Map<String, String>) =
        ComponentParametersFormatter
            .formatFlags(
                sequenceOf(
                    compile,
                    installedBuild,
                    usePak,
                    useIoStore,
                    compressedContent,
                    prerequisites,
                    noCompileUAT,
                    manifests,
                    crashReporter,
                    distribution,
                    verboseLogging,
                    logWindow,
                    noXGE,
                    noDebugInfo,
                    skipEncryption,
                ),
                properties,
            )

    fun formatFlag(
        flag: CheckboxParameter,
        properties: Map<String, String>,
    ) = ComponentParametersFormatter
        .formatFlag(flag, properties)
}
