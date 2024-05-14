package com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun

import arrow.core.NonEmptyList
import com.jetbrains.teamcity.plugins.unrealengine.common.UnrealTargetConfiguration
import com.jetbrains.teamcity.plugins.unrealengine.common.UnrealTargetPlatform

sealed interface BuildConfiguration {
    companion object {
        private fun List<UnrealTargetPlatform>.joinPlatforms() = UnrealTargetPlatformsParameter.joinPlatforms(this)
        private fun List<UnrealBuildTarget>.joinTargets() = UnrealBuildTargetParameter.joinBuildTargets(this)
        private fun List<UnrealTargetConfiguration>.joinTargetConfigurations() = UnrealTargetConfigurationsParameter.joinConfigurations(
            this,
        )
    }

    val arguments: List<String>

    data class Standalone(
        val configuration: NonEmptyList<UnrealTargetConfiguration>,
        val platforms: NonEmptyList<UnrealTargetPlatform>,
        val targets: List<UnrealBuildTarget> = emptyList(),
    ) : BuildConfiguration {
        override val arguments: List<String> = buildList(4) {
            add("-build")
            add("-configuration=${configuration.joinTargetConfigurations()}")
            add("-targetplatform=${platforms.joinPlatforms()}")
            if (targets.any()) {
                add("-target=${targets.joinTargets()}")
            }
        }
    }

    data class Client(
        val configuration: NonEmptyList<UnrealTargetConfiguration>,
        val platforms: NonEmptyList<UnrealTargetPlatform>,
        val targets: List<UnrealBuildTarget> = emptyList(),
    ) : BuildConfiguration {

        override val arguments: List<String> = buildList(5) {
            add("-build")
            add("-client")
            add("-clientconfig=${configuration.joinTargetConfigurations()}")
            add("-targetplatform=${platforms.joinPlatforms()}")
            if (targets.any()) {
                add("-target=${targets.joinTargets()}")
            }
        }
    }

    data class Server(
        val configuration: NonEmptyList<UnrealTargetConfiguration>,
        val platforms: NonEmptyList<UnrealTargetPlatform>,
        val targets: List<UnrealBuildTarget> = emptyList(),
    ) : BuildConfiguration {

        override val arguments: List<String> = buildList(6) {
            add("-build")
            add("-server")
            add("-serverconfig=${configuration.joinTargetConfigurations()}")
            add("-servertargetplatform=${platforms.joinPlatforms()}")
            add("-noclient")
            if (targets.any()) {
                add("-target=${targets.joinTargets()}")
            }
        }
    }

    data class ClientAndServer(
        val clientConfiguration: NonEmptyList<UnrealTargetConfiguration>,
        val clientPlatforms: NonEmptyList<UnrealTargetPlatform>,
        val serverConfiguration: NonEmptyList<UnrealTargetConfiguration>,
        val serverPlatforms: NonEmptyList<UnrealTargetPlatform>,
        val targets: List<UnrealBuildTarget> = emptyList(),
    ) : BuildConfiguration {
        override val arguments: List<String> = buildList(8) {
            add("-build")
            add("-client")
            add("-clientconfig=${clientConfiguration.joinTargetConfigurations()}")
            add("-targetplatform=${clientPlatforms.joinPlatforms()}")
            add("-server")
            add("-serverconfig=${serverConfiguration.joinTargetConfigurations()}")
            add("-servertargetplatform=${serverPlatforms.joinPlatforms()}")
            if (targets.any()) {
                add("-target=${targets.joinTargets()}")
            }
        }
    }
}

@JvmInline
value class UnrealBuildTarget(val value: String)
