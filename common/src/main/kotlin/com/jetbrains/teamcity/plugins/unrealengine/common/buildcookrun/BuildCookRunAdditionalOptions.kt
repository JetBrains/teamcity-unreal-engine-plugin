package com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun

import com.jetbrains.teamcity.plugins.unrealengine.common.parameters.CheckboxParameter
import jetbrains.buildServer.util.StringUtil

object InstalledBuildParameter : CheckboxParameter {
    override val name = "build-cook-run-installed"
    override val displayName = "Installed build"
    override val defaultValue = ""
    override val description = "Use an installed engine build."
    override val advanced = true
}

object CompileParameter : CheckboxParameter {
    override val name = "build-cook-run-compile"
    override val displayName = "Compile"
    override val defaultValue = ""
    override val description = "Compile the targets before running BuildCookRun."
    override val advanced = true
}

object UseIoStoreParameter : CheckboxParameter {
    override val name = "build-cook-run-iostore"
    override val displayName = "IoStore"
    override val defaultValue = ""
    override val description = "Store assets in IoStore containers."
    override val advanced = true
}

object NoCompileUATParameter : CheckboxParameter {
    override val name = "build-cook-run-no-compile-uat"
    override val displayName = "NoCompileUAT"
    override val defaultValue = ""
    override val description = "Skip compiling Unreal Automation Tool."
    override val advanced = true
}

object ManifestsParameter : CheckboxParameter {
    override val name = "build-cook-run-manifests"
    override val displayName = "Manifests"
    override val defaultValue = ""
    override val description = "Generate manifests during packaging."
    override val advanced = true
}

object CrashReporterParameter : CheckboxParameter {
    override val name = "build-cook-run-crash-reporter"
    override val displayName = "CrashReporter"
    override val defaultValue = ""
    override val description = "Include CrashReporter support."
    override val advanced = true
}

object DistributionParameter : CheckboxParameter {
    override val name = "build-cook-run-distribution"
    override val displayName = "Distribution"
    override val defaultValue = ""
    override val description = "Prepare the build for distribution."
    override val advanced = true
}

object VerboseLoggingParameter : CheckboxParameter {
    override val name = "build-cook-run-verbose"
    override val displayName = "Verbose"
    override val defaultValue = ""
    override val description = "Enable verbose logging for BuildCookRun."
    override val advanced = true
}

object LogWindowParameter : CheckboxParameter {
    override val name = "build-cook-run-log-window"
    override val displayName = "Log"
    override val defaultValue = ""
    override val description = "Open the Unreal log window while the command runs."
    override val advanced = true
}

object NoXGEParameter : CheckboxParameter {
    override val name = "build-cook-run-no-xge"
    override val displayName = "NoXGE"
    override val defaultValue = ""
    override val description = "Disable XGE/Incredibuild distributed compilation."
    override val advanced = true
}

object NoDebugInfoParameter : CheckboxParameter {
    override val name = "build-cook-run-no-debug-info"
    override val displayName = "NoDebugInfo"
    override val defaultValue = ""
    override val description = "Skip generating debug symbols for packaged builds."
    override val advanced = true
}

object SkipEncryptionParameter : CheckboxParameter {
    override val name = "build-cook-run-skip-encryption"
    override val displayName = "SkipEncryption"
    override val defaultValue = ""
    override val description = "Disable encryption for pak/IoStore output."
    override val advanced = true
}

data class BuildCookRunAdditionalOptions(
    val installedBuild: Boolean = false,
    val compile: Boolean = false,
    val useIoStore: Boolean = false,
    val noCompileUAT: Boolean = false,
    val manifests: Boolean = false,
    val crashReporter: Boolean = false,
    val distribution: Boolean = false,
    val verboseLogging: Boolean = false,
    val logWindow: Boolean = false,
    val noXGE: Boolean = false,
    val noDebugInfo: Boolean = false,
    val skipEncryption: Boolean = false,
) {
    companion object {
        fun from(runnerParameters: Map<String, String>) =
            BuildCookRunAdditionalOptions(
                runnerParameters[InstalledBuildParameter.name].toBoolean(),
                runnerParameters[CompileParameter.name].toBoolean(),
                runnerParameters[UseIoStoreParameter.name].toBoolean(),
                runnerParameters[NoCompileUATParameter.name].toBoolean(),
                runnerParameters[ManifestsParameter.name].toBoolean(),
                runnerParameters[CrashReporterParameter.name].toBoolean(),
                runnerParameters[DistributionParameter.name].toBoolean(),
                runnerParameters[VerboseLoggingParameter.name].toBoolean(),
                runnerParameters[LogWindowParameter.name].toBoolean(),
                runnerParameters[NoXGEParameter.name].toBoolean(),
                runnerParameters[NoDebugInfoParameter.name].toBoolean(),
                runnerParameters[SkipEncryptionParameter.name].toBoolean(),
            )
    }

    val arguments: List<String> =
        buildList {
            if (installedBuild) add("-installed")
            if (compile) add("-compile")
            if (useIoStore) add("-iostore")
            if (noCompileUAT) add("-nocompileuat")
            if (manifests) add("-manifests")
            if (crashReporter) add("-CrashReporter")
            if (distribution) add("-distribution")
            if (verboseLogging) add("-verbose")
            if (logWindow) add("-log")
            if (noXGE) add("-noxge")
            if (noDebugInfo) add("-nodebuginfo")
            if (skipEncryption) add("-skipencryption")
        }
}

object GenerateChunksParameter : CheckboxParameter {
    override val name = "build-cook-run-generate-chunks"
    override val displayName = "Generate chunks"
    override val defaultValue = ""
    override val description = "Generate chunk manifests during cooking."
    override val advanced = true
}

object IterativeCookingParameter : CheckboxParameter {
    override val name = "build-cook-run-iterative-cooking"
    override val displayName = "Iterative cooking"
    override val defaultValue = ""
    override val description = "Use iterative cooking to only process changed assets."
    override val advanced = true
}

object CookAllParameter : CheckboxParameter {
    override val name = "build-cook-run-cook-all"
    override val displayName = "Cook all"
    override val defaultValue = ""
    override val description = "Cook all available content for the project."
    override val advanced = true
}

object CookMapsOnlyParameter : CheckboxParameter {
    override val name = "build-cook-run-cook-maps-only"
    override val displayName = "Cook maps only"
    override val defaultValue = ""
    override val description = "When cooking all content, restrict cook output to maps and dependencies."
    override val advanced = true
}

object CookPartialGCParameter : CheckboxParameter {
    override val name = "build-cook-run-cook-partial-gc"
    override val displayName = "Cook partial GC"
    override val defaultValue = ""
    override val description = "Allow partial garbage collection while cooking to reduce memory pressure."
    override val advanced = true
}

object FastCookParameter : CheckboxParameter {
    override val name = "build-cook-run-fast-cook"
    override val displayName = "FastCook"
    override val defaultValue = ""
    override val description = "Enable FastCook mode when supported by the target platform."
    override val advanced = true
}

object IgnoreCookErrorsParameter : CheckboxParameter {
    override val name = "build-cook-run-ignore-cook-errors"
    override val displayName = "IgnoreCookErrors"
    override val defaultValue = ""
    override val description = "Continue packaging even if cook reports errors."
    override val advanced = true
}

object SkipCookingEditorContentParameter : CheckboxParameter {
    override val name = "build-cook-run-skip-editor-content"
    override val displayName = "Skip editor content"
    override val defaultValue = ""
    override val description = "Skip cooking editor-only content."
    override val advanced = true
}

object ExcludeEditorContentParameter : CheckboxParameter {
    override val name = "build-cook-run-exclude-editor-content"
    override val displayName = "Exclude editor content"
    override val defaultValue = ""
    override val description = "Exclude editor-only content from the cooked output."
    override val advanced = true
}

object AdditionalCookerOptionsParameter : com.jetbrains.teamcity.plugins.unrealengine.common.parameters.TextInputParameter {
    override val name = "build-cook-run-additional-cooker-options"
    override val displayName = "Additional cooker options"
    override val defaultValue = ""
    override val description = "Additional options passed to `-AdditionalCookerOptions`."
    override val required = false
    override val supportsVcsNavigation = false
    override val expandable = true
    override val advanced = true

    fun from(runnerParameters: Map<String, String>) =
        runnerParameters[name]
            ?.trim()
            ?.takeIf { it.isNotEmpty() }

    fun toArgument(value: String): String {
        val normalized = value.trim().trim('"')
        val escaped = StringUtil.escapeQuotes(normalized)
        return "-AdditionalCookerOptions=\"$escaped\""
    }
}

