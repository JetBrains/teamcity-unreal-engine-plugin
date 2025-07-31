package com.jetbrains.teamcity.plugins.unrealengine.common.commandlets

import arrow.core.raise.Raise
import com.jetbrains.teamcity.plugins.framework.common.raise
import com.jetbrains.teamcity.plugins.unrealengine.common.PropertyValidationError
import com.jetbrains.teamcity.plugins.unrealengine.common.UnrealProjectPath
import com.jetbrains.teamcity.plugins.unrealengine.common.parameters.TextInputParameter
import jetbrains.buildServer.util.StringUtil

object CommandletProjectPathParameter : TextInputParameter {
    override val name = "commandlet-project"
    override val displayName = "Project"
    override val description =
        """
The project to run the editor with. You can specify either the path to a project or its name if the project is "Native".
        """.trimIndent()
    override val defaultValue = ""
    override val required = false
    override val supportsVcsNavigation = true
    override val expandable = false
    override val advanced = false

    fun parseProjectPath(runnerParameters: Map<String, String>): UnrealProjectPath? =
        runnerParameters[name]
            ?.takeIf { it.isNotBlank() }
            ?.let { UnrealProjectPath(it) }
}

object CommandletNameParameter : TextInputParameter {
    override val name = "commandlet-name"
    override val displayName = "Commandlet"
    override val defaultValue = ""
    override val description = "The commandlet name to execute (the value which will be used with \"-run=\")."
    override val required = true
    override val supportsVcsNavigation = false
    override val expandable = false
    override val advanced = false

    context(_: Raise<PropertyValidationError>)
    fun parseCommandlet(runnerParameters: Map<String, String>): Commandlet {
        val commandlet = runnerParameters[name]?.trim()
        if (commandlet.isNullOrEmpty()) {
            raise(PropertyValidationError(name, "The commandlet name is not set."))
        }

        return Commandlet(commandlet)
    }
}

object CommandletArgumentsParameter : TextInputParameter {
    override val name = "commandlet-arguments"
    override val displayName = "Arguments"
    override val defaultValue = ""
    override val description = "Arguments to be passed to the commandlet."
    override val required = false
    override val supportsVcsNavigation = false
    override val expandable = true
    override val advanced = false

    fun parse(runnerParameters: Map<String, String>): List<String> =
        runnerParameters[name]?.let { argumentString ->
            StringUtil
                .splitCommandArgumentsAndUnquote(argumentString)
                .filter { !it.isNullOrBlank() }
        } ?: emptyList()
}
