package com.jetbrains.teamcity.plugins.unrealengine.server.buildgraph

import arrow.core.raise.Raise
import com.jetbrains.teamcity.plugins.unrealengine.common.Error
import com.jetbrains.teamcity.plugins.unrealengine.common.buildgraph.BuildGraphMode
import com.jetbrains.teamcity.plugins.unrealengine.common.buildgraph.BuildGraphModeParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.ensureNotNull
import com.jetbrains.teamcity.plugins.unrealengine.server.extensions.activeRunners
import jetbrains.buildServer.serverSide.SBuild

class BuildGraphSettingsInitializer {
    context(_: Raise<Error>)
    fun initializeBuildSettings(
        originalBuild: SBuild,
        badges: Collection<Badge>,
    ): BuildGraphBuildSettings {
        val originalRunnerParameters =
            originalBuild.buildPromotion
                .activeRunners()
                .singleOrNull()
                ?.parameters
                .let {
                    ensureNotNull(it, "Unable to get runner parameters (there should be exactly one active UnrealEngine runner)")
                }

        val modeSettings = BuildGraphModeParameter.parse(originalRunnerParameters) as? BuildGraphMode.Distributed

        val badgePostingConfig =
            if (modeSettings?.metadataServerUrl == null) {
                BadgePostingConfig.Disabled
            } else {
                ensureNotNull(modeSettings.metadataServerUrl, "Metadata Server URL not set")
                    .let {
                        BadgePostingConfig.Enabled(it, badges)
                    }
            }

        return BuildGraphBuildSettings(badgePostingConfig).also {
            originalBuild.addBuildGraphBuildSettings(it)
        }
    }
}
