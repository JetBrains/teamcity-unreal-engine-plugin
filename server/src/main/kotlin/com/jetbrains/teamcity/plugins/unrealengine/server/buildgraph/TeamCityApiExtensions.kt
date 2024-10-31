package com.jetbrains.teamcity.plugins.unrealengine.server.buildgraph

import arrow.core.getOrElse
import arrow.core.raise.either
import com.jetbrains.teamcity.plugins.unrealengine.common.UnrealCommandType
import com.jetbrains.teamcity.plugins.unrealengine.common.UnrealEngineRunner
import com.jetbrains.teamcity.plugins.unrealengine.common.buildgraph.BuildGraphMode
import com.jetbrains.teamcity.plugins.unrealengine.common.buildgraph.BuildGraphModeParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.parameters.UnrealCommandTypeParameter
import jetbrains.buildServer.serverSide.BuildPromotion
import jetbrains.buildServer.serverSide.BuildPromotionEx
import jetbrains.buildServer.serverSide.BuildTypeSettings
import jetbrains.buildServer.serverSide.SBuildRunnerDescriptor
import jetbrains.buildServer.serverSide.TriggeredByBuilder
import jetbrains.buildServer.serverSide.dependency.DependencyOptions
import jetbrains.buildServer.util.DependencyOptionSupportImpl
import jetbrains.buildServer.virtualConfiguration.generator.VirtualPromotionGeneratorFactory

fun BuildPromotionEx.setRevisionsFrom(another: BuildPromotionEx) =
    setBuildRevisions(
        another.allRevisionsMap.values,
        another.lastModificationId ?: 0L,
        another.chainModificationId ?: 0L,
        false,
    )

fun BuildPromotionEx.asTriggeredBy(): String {
    val triggeredBy = TriggeredByBuilder()
    triggeredBy.addParameters(
        mapOf(
            TriggeredByBuilder.BUILD_TYPE_ID_PARAM_NAME to buildTypeId,
            TriggeredByBuilder.BUILD_ID_PARAM_NAME to id.toString(),
            TriggeredByBuilder.TYPE_PARAM_NAME to "snapshotDependency",
        ),
    )
    return triggeredBy.toString()
}

fun BuildPromotion.hasSingleDistributedBuildGraphStep() = activeRunners().singleOrNull()?.isDistributedBuildGraph() ?: false

fun BuildPromotion.activeRunners(): Collection<SBuildRunnerDescriptor> = buildSettings.buildRunners

private fun SBuildRunnerDescriptor.isDistributedBuildGraph() =
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

fun BuildPromotion.generateIdForVirtualBuild(name: String) = "${id}_ue_plugin_generated_$name"

fun BuildTypeSettings.addUnrealRunner(
    name: String,
    parameters: Map<String, String>,
) = addBuildRunner(name, UnrealEngineRunner.RUN_TYPE, parameters)

fun BuildPromotionEx.markAsGeneratedBy(another: BuildPromotionEx) =
    setAttribute("teamcity.build.unreal-engine.build-graph.generated-by", another.id)

fun BuildPromotionEx.getGeneratedById() =
    this.getAttribute("teamcity.build.unreal-engine.build-graph.generated-by").toString().toLongOrNull()

fun BuildPromotion.asBuildPromotionEx(): BuildPromotionEx = this as BuildPromotionEx
