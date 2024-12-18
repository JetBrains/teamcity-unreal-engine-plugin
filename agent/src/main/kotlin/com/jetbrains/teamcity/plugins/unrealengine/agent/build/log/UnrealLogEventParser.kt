package com.jetbrains.teamcity.plugins.unrealengine.agent.build.log

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import java.time.Clock

class UnrealLogEventParser(
    private val jsonLogEventParser: UnrealJsonLogEventParser,
    private val clock: Clock,
) {
    fun parse(text: String): UnrealLogEvent =
        jsonLogEventParser
            .parse(text)
            ?.let {
                UnrealLogEvent(
                    time = it.time ?: clock.instant(),
                    level = it.level ?: LogLevel.Information,
                    message = it.message,
                    channel = tryToExtractChannel(it),
                )
            } ?: createDefaultEvent(text)

    private fun tryToExtractChannel(event: UnrealJsonLogEvent) =
        (event.properties?.get("_channel") as? JsonObject)
            ?.get("\$text")
            ?.takeIf { it is JsonPrimitive }
            ?.jsonPrimitive
            ?.contentOrNull

    private fun createDefaultEvent(message: String) =
        UnrealLogEvent(
            time = clock.instant(),
            level = LogLevel.Information,
            message = message,
        )
}
