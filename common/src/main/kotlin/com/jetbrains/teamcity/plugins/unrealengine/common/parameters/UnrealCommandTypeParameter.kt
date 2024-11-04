package com.jetbrains.teamcity.plugins.unrealengine.common.parameters

import arrow.core.raise.Raise
import arrow.core.raise.ensureNotNull
import com.jetbrains.teamcity.plugins.unrealengine.common.PropertyValidationError
import com.jetbrains.teamcity.plugins.unrealengine.common.UnrealCommandType
import com.jetbrains.teamcity.plugins.unrealengine.common.enumValueOfOrNull

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
                SelectOption(UnrealCommandType.RunAutomation.name),
                "editRunAutomationProperties.jsp",
                "viewRunAutomationProperties.jsp",
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

    context(Raise<PropertyValidationError>)
    fun parse(runnerParameters: Map<String, String>): UnrealCommandType {
        val commandTypeRaw = runnerParameters[name] ?: raise(PropertyValidationError(name, "Unreal command type is missing"))
        val commandType = enumValueOfOrNull<UnrealCommandType>(commandTypeRaw)
        ensureNotNull(commandType) { PropertyValidationError(name, "Unknown Unreal command $commandTypeRaw") }
        return commandType
    }
}
