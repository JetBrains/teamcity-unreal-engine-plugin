package com.jetbrains.teamcity.plugins.unrealengine.common.parameters

import arrow.core.raise.Raise
import com.jetbrains.teamcity.plugins.framework.common.ensureNotNull
import com.jetbrains.teamcity.plugins.framework.common.raise
import com.jetbrains.teamcity.plugins.unrealengine.common.PropertyValidationError
import com.jetbrains.teamcity.plugins.unrealengine.common.UnrealCommandType
import kotlin.enums.enumEntries

object UnrealCommandTypeParameter : SelectParameter() {
    val commands =
        listOf(
            UnrealCommand(
                SelectOption(UnrealCommandType.BuildCookRun.name),
                "editBuildCookRunProperties.jsp",
                "viewBuildCookRunProperties.jsp",
            ),
            UnrealCommand(
                SelectOption(UnrealCommandType.BuildGraph.name),
                "editBuildGraphProperties.jsp",
                "viewBuildGraphProperties.jsp",
            ),
            UnrealCommand(
                SelectOption(
                    name = UnrealCommandType.RunAutomationTests.value,
                    displayName = UnrealCommandType.RunAutomationTests.name,
                ),
                "editRunAutomationTestsProperties.jsp",
                "viewRunAutomationTestsProperties.jsp",
            ),
            UnrealCommand(
                SelectOption(UnrealCommandType.RunAutomationCommand.name),
                "editAutomationCommandProperties.jsp",
                "viewAutomationCommandProperties.jsp",
            ),
            UnrealCommand(
                SelectOption(UnrealCommandType.RunCommandlet.name),
                "editCommandletProperties.jsp",
                "viewCommandletProperties.jsp",
            ),
        )
    override val description = null
    override val options = commands.map { it.option }

    override val name = "unreal-command"
    override val displayName = "Command"
    override val defaultValue = UnrealCommandType.BuildCookRun.name

    data class UnrealCommand(
        val option: SelectOption,
        val editPage: String,
        val viewPage: String,
    )

    context(_: Raise<PropertyValidationError>)
    fun parse(runnerParameters: Map<String, String>): UnrealCommandType {
        val commandTypeRaw =
            runnerParameters[name] ?: raise(
                PropertyValidationError(
                    name,
                    "Unreal command type is missing",
                ),
            )
        val commandType = enumEntries<UnrealCommandType>().find { it.value == commandTypeRaw }
        ensureNotNull(commandType) { PropertyValidationError(name, "Unknown Unreal command $commandTypeRaw") }
        return commandType
    }
}
