package com.jetbrains.teamcity.plugins.unrealengine.agent

import arrow.core.raise.Raise
import com.jetbrains.teamcity.plugins.framework.common.Environment
import com.jetbrains.teamcity.plugins.framework.common.OSType
import com.jetbrains.teamcity.plugins.framework.common.TeamCityLoggers
import com.jetbrains.teamcity.plugins.unrealengine.common.CommandExecutionContext
import com.jetbrains.teamcity.plugins.unrealengine.common.GenericError
import com.jetbrains.teamcity.plugins.unrealengine.common.UnrealEngineVersion
import com.jetbrains.teamcity.plugins.unrealengine.common.commandlets.EditorExecutableParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.raise

enum class UnrealToolType {
    AutomationTool,
    Editor,
}

data class UnrealTool(
    val executablePath: String,
    val type: UnrealToolType,
)

class UnrealToolRegistry(
    private val engineProvider: UnrealEngineProvider,
    private val environment: Environment,
) {
    companion object {
        private val unreal_version_5 = UnrealEngineVersion(5, 0, 0)
        private val logger = TeamCityLoggers.agent<UnrealToolRegistry>()
    }

    context(Raise<GenericError>, CommandExecutionContext)
    suspend fun automationTool(parameters: Map<String, String>): UnrealTool {
        val engine = engineProvider.findEngine(parameters)

        return UnrealTool(
            getAutomationToolFullPath(engine),
            UnrealToolType.AutomationTool,
        )
    }

    context(CommandExecutionContext, Raise<GenericError>)
    suspend fun editor(parameters: Map<String, String>): UnrealTool {
        val engine = engineProvider.findEngine(parameters)

        val executable = EditorExecutableParameter.parse(parameters)

        return UnrealTool(
            getEditorFullPath(engine, executable?.value),
            UnrealToolType.Editor,
        )
    }

    context(CommandExecutionContext)
    private fun getAutomationToolFullPath(engine: UnrealEngine): String {
        val onWindows = environment.osType == OSType.Windows
        val scriptExtension = if (onWindows) ".bat" else ".sh"

        return resolvePath(
            engine.path.value,
            "Engine",
            "Build",
            "BatchFiles",
            "RunUAT$scriptExtension",
        )
    }

    context(CommandExecutionContext, Raise<GenericError>)
    private fun getEditorFullPath(
        engine: UnrealEngine,
        executable: String? = null,
    ): String {
        if (executable.isNullOrBlank()) {
            val defaultPath =
                resolvePath(
                    engine.path.value,
                    getDefaultRelativePlatformPath(),
                    defaultEditorName(engine).ensureProperExtension(),
                )
            logger.debug("Unreal Editor executable was not explicitly specified. Using the default path: $defaultPath")
            return defaultPath
        }

        return when {
            isAbsolute(executable) -> {
                logger.debug(
                    "Unreal Editor executable was specified as an absolute path ($executable). Using it directly, bypassing engine detection logic",
                )
                resolvePath(executable.ensureProperExtension())
            }
            isFileName(executable) -> {
                val path = resolvePath(engine.path.value, getDefaultRelativePlatformPath(), executable.ensureProperExtension())
                logger.debug(
                    "Unreal Editor executable was specified as a file name ($executable). Attempting to use it with the default path: $path",
                )
                path
            }
            else -> {
                val path = resolvePath(engine.path.value, executable.ensureProperExtension())
                logger.debug(
                    "Unreal Editor executable was specified as a relative path ($executable). Using it relative to the detected Engine root: $path",
                )
                path
            }
        }
    }

    context(CommandExecutionContext)
    private fun isFileName(path: String): Boolean = !path.contains("/") && !path.contains("\\") && !isAbsolute(path) && path.isNotEmpty()

    context(CommandExecutionContext)
    private fun String.ensureProperExtension() =
        when (environment.osType) {
            OSType.Unknown, OSType.Linux, OSType.MacOs -> removeSuffix(".exe")
            OSType.Windows -> ensureSuffix(".exe")
        }

    private fun String.ensureSuffix(suffix: String) = if (this.endsWith(suffix)) this else this + suffix

    context(Raise<GenericError>, CommandExecutionContext)
    private fun getDefaultRelativePlatformPath() = "./Engine/Binaries/${getPlatformFolder()}"

    context(Raise<GenericError>, CommandExecutionContext)
    private fun getPlatformFolder() =
        when (environment.osType) {
            OSType.Windows -> "Win64"
            OSType.MacOs -> "Mac"
            OSType.Linux -> "Linux"
            OSType.Unknown -> raise("Unknown operating system. Unable to get a path to the Editor")
        }

    private fun defaultEditorName(engine: UnrealEngine) = if (engine.version >= unreal_version_5) "UnrealEditor" else "UE4Editor"
}
