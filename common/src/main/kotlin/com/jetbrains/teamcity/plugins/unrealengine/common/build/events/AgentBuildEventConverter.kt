package com.jetbrains.teamcity.plugins.unrealengine.common.build.events

import arrow.core.raise.Raise
import com.jetbrains.teamcity.plugins.unrealengine.common.Error
import com.jetbrains.teamcity.plugins.unrealengine.common.raise
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import kotlinx.serialization.properties.Properties
import kotlinx.serialization.properties.decodeFromStringMap
import kotlinx.serialization.properties.encodeToStringMap

@OptIn(ExperimentalSerializationApi::class)
class AgentBuildEventConverter {
    companion object {
        const val SERVICE_MESSAGE_NAME = "unreal-engine.build.event"

        val serializersModule =
            SerializersModule {
                polymorphic(AgentBuildEvent::class) {
                    subclass(AgentBuildEvent.BuildStepStarted::class)
                    subclass(AgentBuildEvent.BuildStepCompleted::class)
                    subclass(AgentBuildEvent.BuildStepInterrupted::class)
                }
            }
    }

    private val properties = Properties(serializersModule)

    @OptIn(ExperimentalSerializationApi::class)
    context(_: Raise<Error>)
    fun fromMap(map: Map<String, String>): AgentBuildEvent {
        try {
            return properties.decodeFromStringMap<AgentBuildEvent>(map)
        } catch (error: Throwable) {
            raise("Error while parsing event from a map", error)
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    fun toMap(event: AgentBuildEvent): Map<String, String> = properties.encodeToStringMap(event)
}
