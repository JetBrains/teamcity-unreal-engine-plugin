package com.jetbrains.teamcity.plugins.unrealengine.common.ugs

import kotlinx.serialization.Serializable

@Serializable
@JvmInline
value class UgsMetadataServerUrl(
    val value: String,
)
