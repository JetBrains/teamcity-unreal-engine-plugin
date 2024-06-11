package com.jetbrains.teamcity.plugins.unrealengine.server.runner.ui

import com.jetbrains.teamcity.plugins.unrealengine.common.parameters.CheckboxParameter

object ComponentParametersFormatter {
    fun formatFlags(
        flags: Sequence<CheckboxParameter>,
        properties: Map<String, String>,
    ) = flags
        .filter {
            properties.containsKey(it.name)
        }.joinToString(separator = ",") { it.displayName }

    fun formatFlag(
        flag: CheckboxParameter,
        properties: Map<String, String>,
    ): String = if (properties.containsKey(flag.name)) "Yes" else "No"
}
