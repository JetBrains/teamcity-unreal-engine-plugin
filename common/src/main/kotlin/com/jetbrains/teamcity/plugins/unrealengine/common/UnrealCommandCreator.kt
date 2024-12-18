package com.jetbrains.teamcity.plugins.unrealengine.common

import arrow.core.Either
import arrow.core.NonEmptyList
import arrow.core.nonEmptyListOf
import arrow.core.raise.Raise
import arrow.core.raise.either
import com.jetbrains.teamcity.plugins.unrealengine.common.automation.commands.RunAutomationCommand
import com.jetbrains.teamcity.plugins.unrealengine.common.automation.tests.RunAutomationTestsCommand
import com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun.BuildCookRunCommand
import com.jetbrains.teamcity.plugins.unrealengine.common.buildgraph.BuildGraphCommand
import com.jetbrains.teamcity.plugins.unrealengine.common.commandlets.RunCommandletCommand
import com.jetbrains.teamcity.plugins.unrealengine.common.parameters.UnrealCommandTypeParameter

class UnrealCommandCreator {
    context(Raise<NonEmptyList<PropertyValidationError>>)
    fun create(runnerParameters: Map<String, String>): UnrealCommand =
        when (val result = either { UnrealCommandTypeParameter.parse(runnerParameters) }) {
            is Either.Left -> raise(nonEmptyListOf(result.value))
            is Either.Right -> {
                when (result.value) {
                    UnrealCommandType.BuildCookRun -> {
                        BuildCookRunCommand.from(runnerParameters)
                    }
                    UnrealCommandType.BuildGraph -> {
                        BuildGraphCommand.from(runnerParameters)
                    }
                    UnrealCommandType.RunAutomationTests -> {
                        RunAutomationTestsCommand.from(runnerParameters)
                    }
                    UnrealCommandType.RunCommandlet -> {
                        RunCommandletCommand.from(runnerParameters)
                    }
                    UnrealCommandType.RunAutomationCommand -> {
                        RunAutomationCommand.from(runnerParameters)
                    }
                }
            }
        }
}
