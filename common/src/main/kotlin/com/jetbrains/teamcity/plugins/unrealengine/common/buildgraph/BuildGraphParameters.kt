package com.jetbrains.teamcity.plugins.unrealengine.common.buildgraph

import arrow.core.raise.Raise
import com.jetbrains.teamcity.plugins.unrealengine.common.ValidationError
import com.jetbrains.teamcity.plugins.unrealengine.common.enumValueOfOrNull
import com.jetbrains.teamcity.plugins.unrealengine.common.parameters.RunnerParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.parameters.SelectOption
import com.jetbrains.teamcity.plugins.unrealengine.common.parameters.SelectParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.parameters.TextInputParameter

object BuildGraphScriptPathParameter : TextInputParameter {
    override val name = "build-graph-script-path"
    override val displayName = "Script path"
    override val defaultValue = ""
    override val description = "The path to the script describing the build graph."
    override val required = true
    override val supportsVcsNavigation = true
    override val expandable = false
    override val advanced = false

    context(Raise<ValidationError>)
    fun parseScriptPath(properties: Map<String, String>): BuildGraphScriptPath {
        val scriptPath = properties[name]
        if (scriptPath.isNullOrEmpty()) {
            raise(ValidationError(name, "The path to the script is not set."))
        }

        return BuildGraphScriptPath(scriptPath)
    }
}

object BuildGraphTargetNodeParameter : TextInputParameter {
    override val name = "build-graph-target-node"
    override val displayName = "Target"
    override val defaultValue = ""
    override val description = "The name of the node or output tag to be built."
    override val required = true
    override val supportsVcsNavigation = false
    override val expandable = false
    override val advanced = false

    context(Raise<ValidationError>)
    fun parseTargetNode(properties: Map<String, String>): BuildGraphTargetNode {
        val targetNode = properties[name]
        if (targetNode.isNullOrEmpty()) {
            raise(ValidationError(name, "The target node name is not set."))
        }

        return BuildGraphTargetNode(targetNode)
    }
}

object BuildGraphOptionsParameter : RunnerParameter {
    override val name = "build-graph-options"
    override val displayName = "Options"
    override val defaultValue = ""
    val description =
        """
        The newline-delimited list of custom command-line options in the 'OPTION_NAME=OPTION_VALUE' format that
        should be passed to your BuildGraph script.
        """.trimIndent()

    context(Raise<ValidationError>)
    fun parseOptions(properties: Map<String, String>): List<BuildGraphOption> {
        val optionsString = properties[name]
        if (optionsString.isNullOrEmpty()) {
            return emptyList()
        }

        return optionsString
            .split("\r\n", "\r", "\n")
            .filter { it.isNotEmpty() }
            .map { optionString ->
                val nameAndValue = optionString.split("=")
                if (nameAndValue.size != 2) {
                    raise(ValidationError(name, "Make sure all options are in the required 'OPTION_NAME=OPTION_VALUE' format."))
                }

                val name = nameAndValue.first()
                if (name.isEmpty()) {
                    raise(ValidationError(BuildGraphOptionsParameter.name, "All options must have a name."))
                }

                BuildGraphOption(name, nameAndValue.last())
            }
    }
}

object BuildGraphModeParameter : SelectParameter() {
    override val name = "build-graph-mode"
    override val displayName = "Mode"
    override val description =
        """
        ${BuildGraphMode.SingleMachine} - Executes all nodes sequentially on a single build agent. <br>
        ${BuildGraphMode.Distributed} - Distributes the process across multiple agents.
        """.trimIndent()
    override val defaultValue = BuildGraphMode.SingleMachine.toString()
    override val options = BuildGraphMode.entries.map { SelectOption(it.name) }

    fun parse(properties: Map<String, String>): BuildGraphMode {
        val defaultMode = BuildGraphMode.SingleMachine

        val modeRaw = properties[name] ?: return defaultMode

        return enumValueOfOrNull<BuildGraphMode>(modeRaw) ?: defaultMode
    }
}
