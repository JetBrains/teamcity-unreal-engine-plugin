package com.jetbrains.teamcity.plugins.unrealengine.server.extensions

import arrow.core.getOrElse
import arrow.core.raise.either
import com.jetbrains.teamcity.plugins.unrealengine.common.UnrealCommandType
import com.jetbrains.teamcity.plugins.unrealengine.common.UnrealEngineRunner
import com.jetbrains.teamcity.plugins.unrealengine.common.buildgraph.BuildGraphMode
import com.jetbrains.teamcity.plugins.unrealengine.common.buildgraph.BuildGraphModeParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.parameters.UnrealCommandTypeParameter
import jetbrains.buildServer.serverSide.BuildPromotion
import jetbrains.buildServer.serverSide.BuildTypeSettings
import jetbrains.buildServer.serverSide.SBuildRunnerDescriptor
import jetbrains.buildServer.serverSide.dependency.DependencyOptions
import jetbrains.buildServer.util.DependencyOptionSupportImpl
import jetbrains.buildServer.virtualConfiguration.generator.VirtualPromotionGeneratorFactory

fun SBuildRunnerDescriptor.isDistributedBuildGraph() =
    either {
        val unrealRunner = runType.type == UnrealEngineRunner.RUN_TYPE
        val buildGraph = UnrealCommandTypeParameter.parse(parameters) == UnrealCommandType.BuildGraph
        val distributed = BuildGraphModeParameter.parse(parameters) is BuildGraphMode.Distributed
        unrealRunner && buildGraph && distributed
    }.getOrElse { false }

fun DependencyOptionSupportImpl.default(): DependencyOptionSupportImpl {
    setOption(DependencyOptions.TAKE_STARTED_BUILD_WITH_SAME_REVISIONS, true)
    return this
}

fun VirtualPromotionGeneratorFactory.create(build: BuildPromotion): VirtualPromotionGeneratorFactory.VirtualPromotionGenerator =
    create(build, "Unreal Engine BuildGraph")

fun BuildTypeSettings.addUnrealRunner(
    name: String,
    parameters: Map<String, String>,
) = addBuildRunner(name, UnrealEngineRunner.RUN_TYPE, parameters)
