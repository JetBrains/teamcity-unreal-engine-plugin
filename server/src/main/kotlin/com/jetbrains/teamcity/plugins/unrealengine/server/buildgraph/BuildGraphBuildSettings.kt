@file:OptIn(ExperimentalSerializationApi::class)

package com.jetbrains.teamcity.plugins.unrealengine.server.buildgraph

import arrow.core.raise.Raise
import com.jetbrains.teamcity.plugins.unrealengine.common.Error
import com.jetbrains.teamcity.plugins.unrealengine.common.ensureNotNull
import com.jetbrains.teamcity.plugins.unrealengine.common.ugs.UgsMetadataServerUrl
import com.jetbrains.teamcity.plugins.unrealengine.server.build.getUnrealDataStorage
import jetbrains.buildServer.serverSide.SBuild
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.properties.Properties
import kotlinx.serialization.properties.decodeFromStringMap
import kotlinx.serialization.properties.encodeToStringMap

@Serializable
sealed interface BadgePostingConfig {
    @Serializable
    @SerialName("disabled")
    data object Disabled : BadgePostingConfig

    @SerialName("enabled")
    @Serializable
    data class Enabled(
        @SerialName("ugs-metadata-server-url")
        val ugsMetadataServerUrl: UgsMetadataServerUrl,
        val badges: Collection<Badge> = emptyList(),
    ) : BadgePostingConfig
}

@Serializable
data class BuildGraphBuildSettings(
    @SerialName("badge-posting")
    val badgePosting: BadgePostingConfig,
)

private val properties = Properties

fun SBuild.addBuildGraphBuildSettings(settings: BuildGraphBuildSettings) {
    settingStorage.putValues(properties.encodeToStringMap(settings))
}

context(_: Raise<Error>)
fun SBuild.getBuildGraphBuildSettings(): BuildGraphBuildSettings {
    val settingsMap =
        ensureNotNull(
            settingStorage.values,
            "There is no build graph setting associated with a build $buildId",
        )

    return properties.decodeFromStringMap<BuildGraphBuildSettings>(settingsMap)
}

private val SBuild.settingStorage get() = getUnrealDataStorage("build-graph-settings")
