package com.jetbrains.teamcity.plugins.unrealengine.agent

import arrow.core.raise.Raise
import com.jetbrains.teamcity.plugins.framework.common.Environment
import com.jetbrains.teamcity.plugins.framework.common.OSType
import com.jetbrains.teamcity.plugins.unrealengine.common.UnrealEngineVersion

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
    }

    context(UnrealBuildContext, Raise<WorkflowCreationError>)
    suspend fun automationTool(parameters: Map<String, String>) = getTools(parameters).first { it.type == UnrealToolType.AutomationTool }

    context(UnrealBuildContext, Raise<WorkflowCreationError>)
    suspend fun editor(parameters: Map<String, String>) = getTools(parameters).first { it.type == UnrealToolType.Editor }

    context(UnrealBuildContext, Raise<WorkflowCreationError>)
    private suspend fun getTools(parameters: Map<String, String>): Collection<UnrealTool> {
        val engine = engineProvider.findEngine(parameters)

        return listOf(
            UnrealTool(
                getAutomationToolFullPath(engine),
                UnrealToolType.AutomationTool,
            ),
            UnrealTool(
                getEditorFullPath(engine),
                UnrealToolType.Editor,
            ),
        )
    }

    private fun getAutomationToolFullPath(engine: UnrealEngine): String {
        val onWindows = environment.osType == OSType.Windows
        val scriptExtension = if (onWindows) ".bat" else ".sh"
        val separator = if (onWindows) "\\" else "/"

        return sequenceOf(
            engine.path.value,
            "Engine",
            "Build",
            "BatchFiles",
            "RunUAT$scriptExtension",
        ).joinToString(separator = separator)
    }

    context(Raise<WorkflowCreationError>)
    private fun getEditorFullPath(engine: UnrealEngine): String {
        val editorName = if (engine.version >= unreal_version_5) "UnrealEditor" else "UE4Editor"

        return when (environment.osType) {
            OSType.Windows -> "${engine.path.value}\\Engine\\Binaries\\Win64\\$editorName.exe"
            OSType.MacOs -> "${engine.path.value}/Engine/Binaries/Mac/$editorName.app/Contents/MacOS/$editorName"
            OSType.Linux -> "${engine.path.value}/Engine/Binaries/Linux/$editorName"
            OSType.Unknown ->
                raise(
                    WorkflowCreationError.ExecutionPreparationError("Unknown operating system. Unable to get a path to the Editor"),
                )
        }
    }
}
