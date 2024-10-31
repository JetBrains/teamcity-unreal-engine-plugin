package com.jetbrains.teamcity.plugins.unrealengine.server.runner

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.zipOrAccumulate
import com.jetbrains.teamcity.plugins.unrealengine.common.PropertyValidationError
import com.jetbrains.teamcity.plugins.unrealengine.common.UnrealCommandCreator
import com.jetbrains.teamcity.plugins.unrealengine.common.parameters.EngineDetectionModeParameter.parseDetectionMode
import jetbrains.buildServer.serverSide.InvalidProperty
import jetbrains.buildServer.serverSide.PropertiesProcessor

class UnrealEngineRunnerPropertiesValidator(
    private val commandCreator: UnrealCommandCreator,
) : PropertiesProcessor {
    override fun process(properties: MutableMap<String, String>?): MutableCollection<InvalidProperty> {
        properties ?: return mutableListOf()

        val result =
            either {
                zipOrAccumulate<PropertyValidationError, Any, Any, Unit>(
                    { parseDetectionMode(properties) },
                    { commandCreator.create(properties) },
                ) { _, _ -> }
            }

        return when (result) {
            is Either.Left ->
                result.value
                    .map { InvalidProperty(it.propertyName, it.message) }
                    .toMutableList()
            is Either.Right -> mutableListOf()
        }
    }
}
