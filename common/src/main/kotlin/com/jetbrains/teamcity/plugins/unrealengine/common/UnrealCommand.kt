package com.jetbrains.teamcity.plugins.unrealengine.common

import arrow.core.raise.Raise

enum class UnrealCommandType {
    BuildCookRun,
    BuildGraph,
    RunAutomation,
}

interface UnrealCommand {
    context(Raise<GenericError>, CommandExecutionContext)
    fun toArguments(): List<String>
}
