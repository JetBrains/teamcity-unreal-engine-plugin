package com.jetbrains.teamcity.plugins.unrealengine.server.buildgraph

import kotlinx.serialization.Serializable

data class BuildGraph<T>(
    val adjacencyList: Map<T, List<T>>,
    val badges: Collection<Badge>,
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

// This class is annotated with @Serializable because it is intended for serialization
// as part of the build-wide settings.
// Unlike other classes in this file, which are used solely for conversion to a distributed build.
@Serializable
data class Badge(
    val name: String,
    val project: String,
    val nodes: Collection<String>,
)
