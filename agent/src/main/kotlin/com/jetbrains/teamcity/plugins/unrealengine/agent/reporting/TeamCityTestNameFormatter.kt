package com.jetbrains.teamcity.plugins.unrealengine.agent.reporting

import com.jetbrains.teamcity.plugins.framework.common.TeamCityLoggers

// TeamCity test name format
// <suite name>: <package/namespace name>.<class name>.<test method>(<test parameters>)
// <class name> and <test method> cannot have dots in the names
// <package/namespace name> and <class name> cannot have spaces in the names
object TeamCityTestNameFormatter {
    private val logger = TeamCityLoggers.server<TeamCityTestNameFormatter>()

    fun format(name: String, fullName: String): String {
        if (fullName.endsWith(name)) {
            val packageClassName = fullName.removeSuffix(name)
            return sanitizePackageClassName(packageClassName) + sanitizeTestName(name)
        }

        logger.debug(
            "Could not format test name: test path '$fullName' doesn't end with test name '$name', will replace all special characters",
        )
        return sanitizeAll(fullName)
    }

    private const val SUITE_NAME_SEPARATOR = ": "
    private fun sanitizeTestName(text: String) = text
        .replace('.', '_')
        .replace(SUITE_NAME_SEPARATOR, " ")

    private fun sanitizePackageClassName(text: String) = text.replace(' ', '_')

    private fun sanitizeAll(text: String) = sanitizePackageClassName(sanitizeTestName(text))
}
