package com.jetbrains.teamcity.plugins.unrealengine.common.automation.commands

import arrow.core.NonEmptyList
import arrow.core.nonEmptyListOf
import arrow.core.raise.Raise
import com.jetbrains.teamcity.plugins.framework.common.withError
import com.jetbrains.teamcity.plugins.unrealengine.common.CommandExecutionContext
import com.jetbrains.teamcity.plugins.unrealengine.common.GenericError
import com.jetbrains.teamcity.plugins.unrealengine.common.PropertyValidationError
import com.jetbrains.teamcity.plugins.unrealengine.common.UnrealCommand
import com.jetbrains.teamcity.plugins.unrealengine.common.parameters.AdditionalArgumentsParameter

@JvmInline
value class AutomationCommand(
    val value: String,
)

class RunAutomationCommand(
    val command: AutomationCommand,
    val commandArguments: List<String> = emptyList(),
    val extraArguments: List<String> = emptyList(),
) : UnrealCommand {
    companion object {
        context(_: Raise<NonEmptyList<PropertyValidationError>>)
        fun from(runnerParameters: Map<String, String>) =
            withError({ nonEmptyListOf(it) }) {
                val command = AutomationCommandNameParameter.parseCommand(runnerParameters)

                RunAutomationCommand(
                    command,
                    AutomationCommandArgumentsParameter.parse(runnerParameters),
                    AdditionalArgumentsParameter.parse(runnerParameters),
                )
            }
    }

    context(_: Raise<GenericError>, context: CommandExecutionContext)
    override fun toArguments() =
        buildList {
            add(command.value)
            addAll(commandArguments)
            addAll(extraArguments)
        }
}
