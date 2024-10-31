package com.jetbrains.teamcity.plugins.unrealengine.common.buildgraph

class BuildGraphSettings {
    companion object {
        private const val GRAPH_ARTIFACT_NAME = "build-graph-exported.json"
        private const val SETUP_BUILD_TYPE_NAME = "Setup"
        private const val BUILD_GRAPH_GENERATED_MARKER = "teamcity.internal.unreal-engine.build-graph.generated"
    }

    val graphArtifactName = GRAPH_ARTIFACT_NAME
    val setupBuildName = SETUP_BUILD_TYPE_NAME
    val buildGraphGeneratedMarker = BUILD_GRAPH_GENERATED_MARKER
}
