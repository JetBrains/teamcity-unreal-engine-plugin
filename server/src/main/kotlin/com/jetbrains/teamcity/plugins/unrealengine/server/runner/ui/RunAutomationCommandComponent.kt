package com.jetbrains.teamcity.plugins.unrealengine.server.runner.ui

import com.jetbrains.teamcity.plugins.unrealengine.common.automation.commands.AutomationCommandArgumentsParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.automation.commands.AutomationCommandNameParameter

class RunAutomationCommandComponent {
    val command = AutomationCommandNameParameter
    val arguments = AutomationCommandArgumentsParameter
}
