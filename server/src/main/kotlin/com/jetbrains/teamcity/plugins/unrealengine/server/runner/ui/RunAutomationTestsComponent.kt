package com.jetbrains.teamcity.plugins.unrealengine.server.runner.ui

import com.jetbrains.teamcity.plugins.unrealengine.common.automation.tests.AutomationTestsExecCommandParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.automation.tests.AutomationTestsFilterParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.automation.tests.AutomationTestsParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.automation.tests.AutomationTestsProjectPathParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.automation.tests.NullRHIParameter

class RunAutomationTestsComponent {
    val projectPath = AutomationTestsProjectPathParameter
    val nullRHI = NullRHIParameter
    val command = AutomationTestsExecCommandParameter
    val runFilter = AutomationTestsFilterParameter
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
