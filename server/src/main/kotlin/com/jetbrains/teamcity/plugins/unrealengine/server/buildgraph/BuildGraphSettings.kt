package com.jetbrains.teamcity.plugins.unrealengine.server.buildgraph

class BuildGraphSettings {
    companion object {
        private const val GRAPH_ARTIFACT_NAME = "build-graph-exported.json"
        private const val SETUP_BUILD_TYPE_NAME = "Setup"
    }

    val graphArtifactName = GRAPH_ARTIFACT_NAME
    val setupBuildName = SETUP_BUILD_TYPE_NAME
}
