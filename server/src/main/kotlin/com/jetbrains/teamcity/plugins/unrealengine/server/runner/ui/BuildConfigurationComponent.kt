package com.jetbrains.teamcity.plugins.unrealengine.server.runner.ui

import com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun.BuildConfigurationParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun.UnrealBuildTargetParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun.UnrealTargetConfigurationsParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun.UnrealTargetPlatformsParameter

class BuildConfigurationComponent {
    val configuration = BuildConfigurationParameter

    val targetConfigurations = UnrealTargetConfigurationsParameter.Standalone
    val clientTargetConfigurations = UnrealTargetConfigurationsParameter.Client
    val serverTargetConfigurations = UnrealTargetConfigurationsParameter.Server

    val targetPlatforms = UnrealTargetPlatformsParameter.Standalone
    val clientTargetPlatforms = UnrealTargetPlatformsParameter.Client
    val serverTargetPlatforms = UnrealTargetPlatformsParameter.Server

    val buildTargets = UnrealBuildTargetParameter
}
