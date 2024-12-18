package com.jetbrains.teamcity.plugins.unrealengine.agent.build.log

import com.jetbrains.teamcity.plugins.framework.common.TeamCityLoggers
import com.jetbrains.teamcity.plugins.unrealengine.common.JsonEncoder
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonElement
import java.time.Instant
import java.time.format.DateTimeFormatter

@Serializable
enum class LogLevel {
    Trace,
    Debug,
    Information,
    Warning,
    Error,
    Critical,
    None,
}

object InstantSerializer : KSerializer<Instant> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Instant", PrimitiveKind.STRING)

    override fun serialize(
        encoder: Encoder,
        value: Instant,
    ) = encoder.encodeString(value.toString())

    override fun deserialize(decoder: Decoder): Instant {
        val input = decoder.decodeString()
        val normalizedInput = if (input.endsWith("Z")) input else "${input}Z"
        val formatter = DateTimeFormatter.ISO_INSTANT
        return Instant.from(formatter.parse(normalizedInput))
    }
}

@Serializable
data class UnrealJsonLogEvent(
    @Serializable(with = InstantSerializer::class)
    val time: Instant? = null,
    val level: LogLevel? = null,
    val message: String,
    val properties: Map<String, JsonElement>? = null,
)

class UnrealJsonLogEventParser {
    companion object {
        private val json = JsonEncoder.instance
        private val logger = TeamCityLoggers.agent<UnrealJsonLogEventParser>()
    }

    fun parse(text: String): UnrealJsonLogEvent? {
        if (text.isBlank() || text[0] != '{') {
            logger.debug("Line \"$text\" doesn't look like a valid JSON, giving up parsing")
            return null
        }

        return runCatching {
            json.decodeFromString<UnrealJsonLogEvent>(text)
        }.getOrElse {
            logger.debug("Unable to parse given text: \"$text\" into a structured JSON log event", it)
            null
        }
    }
}
