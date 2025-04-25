package com.jetbrains.teamcity.plugins.unrealengine.server.extensions

import jetbrains.buildServer.serverSide.BuildPromotion
import jetbrains.buildServer.serverSide.BuildPromotionEx
import jetbrains.buildServer.serverSide.SBuildRunnerDescriptor
import jetbrains.buildServer.serverSide.TriggeredByBuilder
import jetbrains.buildServer.util.DependencyOptionSupportImpl

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

fun BuildPromotion.generateIdForVirtualBuild(name: String) = "${id}_ue_plugin_generated_$name"

fun BuildPromotionEx.markAsGeneratedBy(another: BuildPromotionEx) =
    setAttribute("teamcity.build.unreal-engine.build-graph.generated-by", another.id)

fun BuildPromotionEx.getGeneratedById() =
    this.getAttribute("teamcity.build.unreal-engine.build-graph.generated-by").toString().toLongOrNull()

fun BuildPromotionEx.addDependencies(
    dependencies: Collection<BuildPromotionEx>,
    optionConfig: (DependencyOptionSupportImpl.() -> Unit)? = null,
) {
    val options =
        DependencyOptionSupportImpl()
            .default()
            .also { optionConfig?.invoke(it) }

    for (dependency in dependencies) {
        addDependency(dependency, options)
    }
}

fun BuildPromotion.asBuildPromotionEx(): BuildPromotionEx = this as BuildPromotionEx
