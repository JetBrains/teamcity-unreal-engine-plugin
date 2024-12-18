package com.jetbrains.teamcity.plugins.unrealengine.common.automation.commands

import arrow.core.raise.Raise
import com.jetbrains.teamcity.plugins.unrealengine.common.PropertyValidationError
import com.jetbrains.teamcity.plugins.unrealengine.common.parameters.TextInputParameter
import jetbrains.buildServer.util.StringUtil

object AutomationCommandNameParameter : TextInputParameter {
    override val name = "automation-command-name-parameter"
    override val displayName = "Name"
    override val defaultValue = ""
    override val description = "The automation command name to execute."
    override val required = true
    override val supportsVcsNavigation = false
    override val expandable = false
    override val advanced = false

    context(Raise<PropertyValidationError>)
    fun parseCommand(runnerParameters: Map<String, String>): AutomationCommand {
        val command = runnerParameters[name]?.trim()

        if (command.isNullOrEmpty()) {
            raise(PropertyValidationError(name, "The command name is not set."))
        }

        return AutomationCommand(command)
    }
}

object AutomationCommandArgumentsParameter : TextInputParameter {
    override val name = "automation-command-arguments"
    override val displayName = "Arguments"
    override val defaultValue = ""
    override val description = "Arguments to be passed to the automation command."
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
