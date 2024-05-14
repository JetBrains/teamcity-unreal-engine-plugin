package com.jetbrains.teamcity.plugins.unrealengine.server.runner.ui

import com.jetbrains.teamcity.plugins.unrealengine.common.parameters.EngineDetectionModeParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.parameters.UnrealEngineIdentifierParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.parameters.UnrealEngineRootParameter

class EngineComponent {
    val detectionMode = EngineDetectionModeParameter
    val engineRootPath = UnrealEngineRootParameter
    val engineIdentifier = UnrealEngineIdentifierParameter
}
