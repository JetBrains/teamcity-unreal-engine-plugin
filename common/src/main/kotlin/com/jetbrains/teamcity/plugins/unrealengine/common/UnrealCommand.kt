package com.jetbrains.teamcity.plugins.unrealengine.common

import arrow.core.Either

enum class UnrealCommandType {
    BuildCookRun,
    BuildGraph,
    RunAutomation,
}

@JvmInline
value class ArgumentsPreparationError(val message: String)

interface UnrealCommand {
    context(CommandExecutionContext)
    fun toArguments(): Either<ArgumentsPreparationError, List<String>>
}
