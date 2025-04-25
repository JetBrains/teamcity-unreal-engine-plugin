package com.jetbrains.teamcity.plugins.unrealengine.server.buildgraph

import arrow.core.raise.Raise
import com.jetbrains.teamcity.plugins.unrealengine.common.Error
import com.jetbrains.teamcity.plugins.unrealengine.common.buildgraph.BuildGraphRunnerInternalSettings
import com.jetbrains.teamcity.plugins.unrealengine.common.buildgraph.toMap
import com.jetbrains.teamcity.plugins.unrealengine.common.parameters.AdditionalArgumentsParameter
import com.jetbrains.teamcity.plugins.unrealengine.server.build.DistributedBuild
import com.jetbrains.teamcity.plugins.unrealengine.server.extensions.activeRunners
import com.jetbrains.teamcity.plugins.unrealengine.server.extensions.addDependencies
import com.jetbrains.teamcity.plugins.unrealengine.server.extensions.addUnrealRunner
import jetbrains.buildServer.requirements.Requirement
import jetbrains.buildServer.requirements.RequirementType
import jetbrains.buildServer.serverSide.BuildPromotionEx
import jetbrains.buildServer.serverSide.SBuild
import jetbrains.buildServer.serverSide.SRunningBuild

class BuildGraphDistributedBuildCreator(
    private val virtualBuildCreator: BuildGraphVirtualBuildCreator,
) {
    context(Raise<Error>)
    fun create(
        originalBuild: SBuild,
        buildGraph: BuildGraph<BuildGraphNodeGroup>,
    ): DistributedBuild =
        with(virtualBuildCreator.inContextOf(originalBuild.buildPromotion)) {
            createDistributedBuild(buildGraph)
        }

    context(BuildGraphVirtualBuildCreator.VirtualBuildCreationContext)
    private fun createDistributedBuild(buildGraph: BuildGraph<BuildGraphNodeGroup>): DistributedBuild {
        val groupDependencies = mutableMapOf<BuildGraphNodeGroup, MutableList<BuildPromotionEx>>()

        val buildsToAdd =
            buildGraph
                .topologicalSort()
                .map {
                    val build = createBuildForGroupOfNodes(it)

                    for (successor in buildGraph.adjacencyList[it]!!) {
                        groupDependencies.computeIfAbsent(successor) { mutableListOf() }
                        groupDependencies[successor]!!.add(build)
                    }
                    build.addDependencies(groupDependencies[it].orEmpty())
                    build
                }

        return DistributedBuild(buildsToAdd)
    }

    context(BuildGraphVirtualBuildCreator.VirtualBuildCreationContext)
    private fun createBuildForGroupOfNodes(group: BuildGraphNodeGroup): BuildPromotionEx {
        val originalRunnerParameters = originalBuild.activeRunners().single().parameters
        val originalBuildId = originalBuild.id.toString()

        return virtualBuildCreator.create(group.name) {
            for (node in group.nodes) {
                val parameters =
                    buildMap {
                        putAll(originalRunnerParameters)
                        executeSingleNode(node.name)
                        addInternalGraphSettings(originalBuildId)
                    }

                addUnrealRunner(node.name, parameters)
            }

            // This cast is safe because we're operating on a finishing build (Setup),
            // which already has its build number resolved.
            buildNumberPattern = (originalBuild.associatedBuild as SRunningBuild).buildNumber

            if (group.agents.any()) {
                addRequirement(
                    Requirement(
                        "unreal-engine.build-graph.agent.type",
                        ".*(;|^)(${group.agents.joinToString(separator = "|")})(;|$).*",
                        RequirementType.MATCHES,
                    ),
                )
            }
        }
    }

    private fun MutableMap<String, String>.executeSingleNode(name: String) =
        put(AdditionalArgumentsParameter.name, get(AdditionalArgumentsParameter.name) + " \"-SingleNode=$name\"")

    private fun MutableMap<String, String>.addInternalGraphSettings(originalBuildId: String) =
        putAll(BuildGraphRunnerInternalSettings.RegularBuildSettings(originalBuildId).toMap())
}
