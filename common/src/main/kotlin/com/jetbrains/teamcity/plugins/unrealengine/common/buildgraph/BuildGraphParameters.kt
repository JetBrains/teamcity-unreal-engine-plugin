package com.jetbrains.teamcity.plugins.unrealengine.common.buildgraph

import arrow.core.raise.Raise
import arrow.core.raise.ensure
import com.jetbrains.teamcity.plugins.unrealengine.common.PropertyValidationError
import com.jetbrains.teamcity.plugins.unrealengine.common.parameters.CheckboxParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.parameters.RunnerParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.parameters.SelectOption
import com.jetbrains.teamcity.plugins.unrealengine.common.parameters.SelectParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.parameters.TextInputParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.ugs.UgsMetadataServerUrl

object BuildGraphScriptPathParameter : TextInputParameter {
    override val name = "build-graph-script-path"
    override val displayName = "Script path"
    override val defaultValue = ""
    override val description = "The path to the script describing the build graph."
    override val required = true
    override val supportsVcsNavigation = true
    override val expandable = false
    override val advanced = false

    context(Raise<PropertyValidationError>)
    fun parseScriptPath(properties: Map<String, String>): BuildGraphScriptPath {
        val scriptPath = properties[name]
        if (scriptPath.isNullOrEmpty()) {
            raise(PropertyValidationError(name, "The path to the script is not set."))
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

    context(Raise<PropertyValidationError>)
    fun parseTargetNode(properties: Map<String, String>): BuildGraphTargetNode {
        val targetNode = properties[name]
        if (targetNode.isNullOrEmpty()) {
            raise(PropertyValidationError(name, "The target node name is not set."))
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

    context(Raise<PropertyValidationError>)
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
                    raise(PropertyValidationError(name, "Make sure all options are in the required 'OPTION_NAME=OPTION_VALUE' format."))
                }

                val name = nameAndValue.first()
                if (name.isEmpty()) {
                    raise(PropertyValidationError(BuildGraphOptionsParameter.name, "All options must have a name."))
                }

                BuildGraphOption(name, nameAndValue.last())
            }
    }
}

object BuildGraphModeParameter : SelectParameter() {
    val distributed = SelectOption("Distributed")
    private val singleMachine = SelectOption("SingleMachine")

    override val name = "build-graph-mode"
    override val displayName = "Mode"
    override val description =
        """
        ${singleMachine.name} - Executes all nodes sequentially on a single build agent.
        ${distributed.name} - Distributes the process across multiple agents.
        """.trimIndent()
    override val defaultValue = singleMachine.name
    override val options = listOf(singleMachine, distributed)

    context(Raise<PropertyValidationError>)
    fun parse(properties: Map<String, String>): BuildGraphMode {
        val modeRaw = properties[name] ?: return BuildGraphMode.SingleMachine

        return when (modeRaw) {
            singleMachine.name -> BuildGraphMode.SingleMachine
            distributed.name -> {
                val postBadges = properties[PostBadgesFromGraphParameter.name].toBoolean()
                val metadataServerUrl = properties[UgsMetadataServerUrlParameter.name]
                if (postBadges) {
                    ensure(!metadataServerUrl.isNullOrBlank()) {
                        PropertyValidationError(UgsMetadataServerUrlParameter.name, "Metadata server URL should not be empty")
                    }
                }
                BuildGraphMode.Distributed(if (postBadges) UgsMetadataServerUrl(metadataServerUrl!!) else null)
            }
            else -> raise(PropertyValidationError(name, "Unknown BuildGraph mode value $modeRaw"))
        }
    }
}

object PostBadgesFromGraphParameter : CheckboxParameter {
    override val description = "Enables posting of badges defined in the build graph"
    override val advanced = false
    override val name = "build-graph-post-badges"
    override val displayName = "Post badges"
    override val defaultValue = false.toString()
}

object UgsMetadataServerUrlParameter : TextInputParameter {
    override val description =
        """
        Specify the metadata server address where badges will be posted.
        Example: http://localhost:1111/ugs-metadata-server
        """.trimIndent()
    override val supportsVcsNavigation = false
    override val expandable = false
    override val required = true
    override val advanced = false
    override val name = "ugs-metadata-server"
    override val displayName = "Metadata server"
    override val defaultValue = ""
}
