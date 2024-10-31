package com.jetbrains.teamcity.plugins.unrealengine.common

import com.jetbrains.teamcity.plugins.unrealengine.common.build.events.AgentBuildEventConverter
import kotlinx.serialization.json.Json

object JsonEncoder {
    val instance =
        Json {
            ignoreUnknownKeys = true
            serializersModule = AgentBuildEventConverter.serializersModule
        }
}
