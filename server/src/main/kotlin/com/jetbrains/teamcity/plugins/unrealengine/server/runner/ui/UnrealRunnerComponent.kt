package com.jetbrains.teamcity.plugins.unrealengine.server.runner.ui

import com.jetbrains.teamcity.plugins.unrealengine.common.parameters.AdditionalArgumentsParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.parameters.UnrealCommandTypeParameter

class UnrealRunnerComponent {
    val commandType = UnrealCommandTypeParameter
    val additionalArguments = AdditionalArgumentsParameter
}
