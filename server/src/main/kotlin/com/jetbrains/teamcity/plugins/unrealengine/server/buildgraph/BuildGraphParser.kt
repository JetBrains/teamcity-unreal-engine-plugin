package com.jetbrains.teamcity.plugins.unrealengine.server.buildgraph

import arrow.core.raise.Raise
import com.jetbrains.teamcity.plugins.unrealengine.common.Error
import com.jetbrains.teamcity.plugins.unrealengine.common.JsonEncoder
import com.jetbrains.teamcity.plugins.unrealengine.common.raise
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.decodeFromStream
import java.io.InputStream

@Serializable
private data class ExportedNode(
    @SerialName("Name")
    val name: String,
    @SerialName("DependsOn")
    val dependsOn: String,
    /**
     * Whether to start this node as soon as its dependencies are satisfied, rather than waiting for all of its agent's dependencies to be met.
     */
    @SerialName("RunEarly")
    val runEarly: Boolean,
)

@Serializable
private data class ExportedGroup(
    @SerialName("Name")
    val name: String,
    /**
     * Array of valid agent types that these nodes may run on. When running in the build system, this determines the class of machine that should
     * be selected to run these nodes. The first defined agent type for this branch will be used.
     */
    @SerialName("Agent Types")
    val agents: Collection<String>,
    @SerialName("Nodes")
    val nodes: Collection<ExportedNode>,
)

@Serializable
private data class ExportedBadge(
    @SerialName("Name")
    val name: String,
    @SerialName("Project")
    val project: String,
    @SerialName("AllDependencies")
    val allDependencies: String,
    @SerialName("DirectDependencies")
    val directDependencies: String,
)

@Serializable
private data class ExportedBuildGraph(
    @SerialName("Groups")
    val groups: Collection<ExportedGroup>,
    @SerialName("Badges")
    val badges: Collection<ExportedBadge>,
)

class BuildGraphParser {
    companion object {
        private val json = JsonEncoder.instance
    }

    context(Raise<Error>)
    @OptIn(ExperimentalSerializationApi::class)
    fun parse(stream: InputStream): BuildGraph<BuildGraphNodeGroup> {
        val buildGraph =
            try {
                json.decodeFromStream<ExportedBuildGraph>(stream)
            } catch (e: Throwable) {
                raise("An error occurred while parsing exported build graph file", e)
            }

        val graph = buildGraph.groups.associateWith { mutableListOf<ExportedGroup>() }
        val nodeToGroup = buildGraph.groups.flatMap { group -> group.nodes.map { it.name to group } }.toMap()

        for (group in buildGraph.groups) {
            for (node in group.nodes) {
                for (nodeDependencyName in node.dependencies()) {
                    val dependencyGroup = nodeToGroup[nodeDependencyName]!!

                    if (dependencyGroup == group) {
                        continue
                    }

                    graph[dependencyGroup]!!.add(group)
                }
            }
        }

        return BuildGraph(
            graph
                .map { (group, successorGroup) ->
                    group.toNodeGroup() to successorGroup.map { it.toNodeGroup() }
                }.toMap(),
            buildGraph.badges.map { it.toBadge() },
        )
    }

    private fun ExportedBadge.toBadge() =
        Badge(
            name,
            project,
            allDependencies.split(';').filter { it.isNotEmpty() },
        )

    private fun ExportedNode.dependencies(): List<String> = dependsOn.split(';').filter { it.isNotEmpty() }

    private fun ExportedGroup.toNodeGroup(): BuildGraphNodeGroup =
        BuildGraphNodeGroup(name, agents, nodes.map { BuildGraphNode(it.name, it.dependencies()) })
}
