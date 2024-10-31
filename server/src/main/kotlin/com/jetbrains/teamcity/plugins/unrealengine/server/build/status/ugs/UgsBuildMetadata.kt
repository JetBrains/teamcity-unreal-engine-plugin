package com.jetbrains.teamcity.plugins.unrealengine.server.build.status.ugs

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.reflect.KClass

@Serializable
data class UgsBuildMetadata(
    @SerialName("ChangeNumber")
    val change: Long,
    @SerialName("Project")
    val project: String,
    @SerialName("BuildType")
    val badgeName: String,
    @SerialName("Url")
    val url: String,
    @SerialName("Result")
    val badgeState: BadgeState,
)

@Serializable(with = BadgeStateAsIntSerializer::class)
enum class BadgeState {
    Starting,
    Failure,
    Warning,
    Success,
    Skipped,
}

object BadgeStateAsIntSerializer : EnumAsIntSerializer<BadgeState>(BadgeState::class)

open class EnumAsIntSerializer<T : Enum<T>>(
    private val enumClass: KClass<T>,
) : KSerializer<T> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(enumClass.java.name, PrimitiveKind.INT)

    override fun deserialize(decoder: Decoder): T = enumClass.java.enumConstants[decoder.decodeInt()]

    override fun serialize(
        encoder: Encoder,
        value: T,
    ) = encoder.encodeInt(value.ordinal)
}
