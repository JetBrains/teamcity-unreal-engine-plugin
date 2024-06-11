package com.jetbrains.teamcity.plugins.unrealengine.server.runner.ui

import com.jetbrains.teamcity.plugins.unrealengine.common.automation.AutomationExecCommandParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.automation.AutomationFilterParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.automation.AutomationProjectPathParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.automation.AutomationTestsParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.automation.NullRHIParameter

class RunAutomationComponent {
    val projectPath = AutomationProjectPathParameter
    val nullRHI = NullRHIParameter
    val command = AutomationExecCommandParameter
    val runFilter = AutomationFilterParameter
    val runTests = AutomationTestsParameter

    fun describeFlags(properties: Map<String, String>) =
        ComponentParametersFormatter
            .formatFlags(
                sequenceOf(
                    nullRHI,
                ),
                properties,
            )
}
