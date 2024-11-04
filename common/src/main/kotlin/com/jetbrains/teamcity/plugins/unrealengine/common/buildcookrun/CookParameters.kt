package com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun

import com.jetbrains.teamcity.plugins.unrealengine.common.parameters.CheckboxParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.parameters.TextInputParameter

object CookStageSwitchParameter : CheckboxParameter {
    override val name = "build-cook-run-cook"
    override val displayName = "Cook"
    override val defaultValue = true.toString()
    override val description = "Perform cooking"
    override val advanced = false

    fun parseCookOptions(runnerParameters: Map<String, String>) =
        runnerParameters[name]?.toBooleanStrictOrNull()?.let {
            CookOptions.from(runnerParameters)
        }
}

object CookCulturesParameter : TextInputParameter {
    private const val SEPARATOR = "+"

    override val name = "build-cook-run-cook-cultures"
    override val displayName = "Cook cultures"
    override val defaultValue = ""
    override val required = false
    override val description =
        """
        The list of localization cultures (separated by '+') that should be included. For example, 'en-US + de-DE'.
        If none are specified, cultures from the CulturesToStage section of project settings are used.
        """.trimIndent()
    override val supportsVcsNavigation = false
    override val expandable = true
    override val advanced = true

    fun from(runnerParameters: Map<String, String>) =
        runnerParameters[name]
            ?.splitToSequence(SEPARATOR)
            ?.map { CookCulture(it) }
            ?.toList()
}

object MapsToCookParameter : TextInputParameter {
    private const val SEPARATOR = "+"

    override val name = "build-cook-run-maps-to-cook"
    override val displayName = "Maps to cook"
    override val defaultValue = ""
    override val required = false
    override val description =
        """
        The list of map names (separated by '+') to include.
        If this list is empty, maps specified in the project settings will be used.
        """.trimIndent()
    override val supportsVcsNavigation = false
    override val expandable = true
    override val advanced = true

    fun from(runnerParameters: Map<String, String>) =
        runnerParameters[name]
            ?.splitToSequence(SEPARATOR)
            ?.map { CookMap(it) }
            ?.toList()
}

object UnversionedCookedContentParameter : CheckboxParameter {
    override val name = "build-cook-run-unversioned-cooked-content"
    override val displayName = "Unversioned content"
    override val defaultValue = true.toString()
    override val description = "Enables omitting asset versions, assuming all loaded assets are of the current version."
    override val advanced = true

    fun from(runnerParameters: Map<String, String>) = runnerParameters[name].toBoolean()
}
