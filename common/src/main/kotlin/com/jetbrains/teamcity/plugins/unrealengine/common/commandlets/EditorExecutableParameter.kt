package com.jetbrains.teamcity.plugins.unrealengine.common.commandlets

import com.jetbrains.teamcity.plugins.unrealengine.common.parameters.TextInputParameter

object EditorExecutableParameter : TextInputParameter {
    override val name = "unreal-editor-executable"
    override val displayName = "Editor"
    override val defaultValue = ""
    override val description =
        """
        Specify the path or binary name for the Unreal Engine Editor. You can:
        • Leave it blank to use the default editor.
        • Provide a binary name to use it from the default path.
        • Specify a relative path to use it relative to the engine root directory.
        • Specify an absolute path to use it directly, bypassing engine detection logic.
        """.trimIndent()
    override val required = false
    override val supportsVcsNavigation = false
    override val expandable = false
    override val advanced = true

    fun parse(runnerParameters: Map<String, String>): UnrealEditorExecutable? =
        runnerParameters[name]
            ?.takeIf { it.isNotBlank() }
            ?.let { UnrealEditorExecutable(it) }
}
