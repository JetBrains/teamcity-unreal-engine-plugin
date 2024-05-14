package com.jetbrains.teamcity.plugins.framework.resource.location.windows.registry

import com.jetbrains.teamcity.plugins.framework.common.TeamCityLoggers

internal class WindowsRegistryParser {
    companion object {
        private val logger = TeamCityLoggers.get<WindowsRegistryParser>()
        private val valuePattern = Regex(
            "^(\\s{4}|\\s*\\t)(.+)(\\s{4}|\\t)(${
                WindowsRegistryValueType.entries.joinToString(separator = "|") { it.id }
            })(\\s{4}|\\t)(.*)\$"
        )
    }

    fun tryParseKey(text: String): WindowsRegistryEntry.Key? {
        val hivePrefix = "HKEY"
        return if (text.startsWith(hivePrefix)) {
            logger.debug("Line matches registry key pattern. Line: $text")
            WindowsRegistryEntry.Key(text)
        } else {
            logger.debug("Line doesn't match registry key pattern. Line: $text")
            null
        }
    }

    fun tryParseValue(text: String): WindowsRegistryEntry.Value? {
        val matchResult = valuePattern.matchEntire(text)

        return if (matchResult != null) {
            logger.debug("Line matches registry value pattern. Line: $text")
            val (_, name, _, type, _, data) = matchResult.destructured
            WindowsRegistryEntry.Value(name, WindowsRegistryValueType.tryParse(type)!!, data)
        } else {
            logger.debug("Line doesn't match registry value pattern. Line: $text")
            null
        }
    }
}
