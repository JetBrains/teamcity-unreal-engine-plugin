package com.jetbrains.teamcity.plugins.unrealengine.server.buildgraph

data class BuildGraph<T>(
    val adjacencyList: Map<T, List<T>>,
)

data class BuildGraphNodeGroup(
    val name: String,
    val agents: Collection<String>,
    val nodes: Collection<BuildGraphNode>,
)

data class BuildGraphNode(
    val name: String,
    val dependsOn: Collection<String>,
)

data class BuildGraphConfigurationError(
    val message: String,
    val exception: Throwable? = null,
)
