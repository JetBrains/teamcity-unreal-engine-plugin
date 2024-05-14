package com.jetbrains.teamcity.plugins.unrealengine.common

sealed interface EngineDetectionMode {
    data class Automatic(val identifier: UnrealEngineIdentifier) : EngineDetectionMode

    data class Manual(val engineRootPath: UnrealEngineRootPath) : EngineDetectionMode
}
