package com.jetbrains.teamcity.plugins.unrealengine.common.parameters

import com.jetbrains.teamcity.plugins.unrealengine.common.escapeHTML
import jetbrains.buildServer.util.StringUtil

interface RunnerParameter {
    val name: String
    val displayName: String
    val defaultValue: String
}

interface TextInputParameter : RunnerParameter {
    val description: String?
    val supportsVcsNavigation: Boolean
    val expandable: Boolean
    val required: Boolean
    val advanced: Boolean
}

abstract class SelectParameter : RunnerParameter {
    abstract val description: String?
    abstract val options: List<SelectOption>

    val optionNamesAsJsArray by lazy { "[${options.map { it.name }.joinToString(separator = ",") { "'$it'" }}]" }

    fun escapeHTML(value: String) = value.escapeHTML()
}

interface CheckboxParameter : RunnerParameter {
    val description: String
    val advanced: Boolean
}

abstract class MultiSelectParameter : SelectParameter() {
    abstract val required: Boolean
    abstract val allowCustomValues: Boolean

    fun getOptionsAsJsArray() = options.toJsArray()

    fun getSelectedOptionsAsJsArray(properties: Map<String, String>): String {
        val selectedValues = properties[name]?.split(separator).orEmpty()
        val knownSelectedOptions = options.filter { selectedValues.contains(it.name) }

        val customSelectedOptions =
            selectedValues
                .filter { !knownSelectedOptions.any { option -> option.name == it } }
                .map { SelectOption(it, it) }

        val selectedOptions = knownSelectedOptions + customSelectedOptions

        return selectedOptions.toJsArray()
    }

    open val separator = ","

    private fun Collection<SelectOption>.toJsArray() =
        "[${joinToString(separator = ",") { "{key: \"${it.name}\", label: \"${it.displayName}\"}"}}]"
}

data class SelectOption(
    val name: String,
    val displayName: String = name,
)

object AdditionalArgumentsParameter : TextInputParameter {
    override val name = "additional-arguments"
    override val defaultValue = DefaultAdditionalParameters.allToString()
    override val displayName = "Additional arguments"
    override val description = "Additional command line arguments."
    override val required = false
    override val supportsVcsNavigation = false
    override val expandable = true
    override val advanced = true

    fun parse(runnerParameters: Map<String, String>): List<String> =
        runnerParameters[name]?.let { argumentString ->
            StringUtil
                .splitCommandArgumentsAndUnquote(argumentString)
                .filter { !it.isNullOrBlank() }
        } ?: emptyList()
}
