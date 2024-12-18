package com.jetbrains.teamcity.plugins.unrealengine.common

import arrow.core.raise.Raise

enum class UnrealCommandType(
    val value: String,
) {
    BuildCookRun("BuildCookRun"),
    BuildGraph("BuildGraph"),
    RunAutomationTests("RunAutomation"),
    RunCommandlet("RunCommandlet"),
    RunAutomationCommand("RunAutomationCommand"),
}

interface UnrealCommand {
    context(Raise<GenericError>, CommandExecutionContext)
    fun toArguments(): List<String>
}
