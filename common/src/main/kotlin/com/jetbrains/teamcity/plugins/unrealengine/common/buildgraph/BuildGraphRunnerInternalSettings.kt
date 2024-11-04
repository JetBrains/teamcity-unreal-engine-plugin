@file:OptIn(ExperimentalSerializationApi::class)

package com.jetbrains.teamcity.plugins.unrealengine.common.buildgraph

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import kotlinx.serialization.properties.Properties
import kotlinx.serialization.properties.decodeFromStringMap
import kotlinx.serialization.properties.encodeToStringMap

@OptIn(ExperimentalSerializationApi::class)
private val properties =
    Properties(
        SerializersModule {
            polymorphic(BuildGraphRunnerInternalSettings::class) {
                subclass(BuildGraphRunnerInternalSettings.SetupBuildSettings::class)
                subclass(BuildGraphRunnerInternalSettings.RegularBuildSettings::class)
            }
        },
    )

private const val PROPERTY_KEY_PREFIX = "build-graph.internal-settings."

sealed interface BuildGraphRunnerInternalSettings {
    companion object {
        fun fromRunnerParameters(parametersMap: Map<String, String>): BuildGraphRunnerInternalSettings {
            val settingsSubMap =
                parametersMap
                    .filter { it.key.startsWith(PROPERTY_KEY_PREFIX) }
                    .mapKeys {
                        it.key.substring(PROPERTY_KEY_PREFIX.length)
                    }

            return properties.decodeFromStringMap<BuildGraphRunnerInternalSettings>(settingsSubMap)
        }
    }

    @Serializable
    @SerialName("setup")
    data class SetupBuildSettings(
        @SerialName("exported-graph-path")
        val exportedGraphPath: String,
        @SerialName("composite-build-id")
        val compositeBuildId: String,
    ) : BuildGraphRunnerInternalSettings

    @Serializable
    @SerialName("regular")
    data class RegularBuildSettings(
        @SerialName("composite-build-id")
        val compositeBuildId: String,
    ) : BuildGraphRunnerInternalSettings
}

fun BuildGraphRunnerInternalSettings.toMap() =
    properties
        .encodeToStringMap(this)
        .mapKeys {
            "${PROPERTY_KEY_PREFIX}${it.key}"
        }
