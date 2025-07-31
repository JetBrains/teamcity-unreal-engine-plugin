package com.jetbrains.teamcity.plugins.unrealengine.server.buildgraph

import arrow.core.raise.Raise
import com.jetbrains.teamcity.plugins.unrealengine.common.Error
import com.jetbrains.teamcity.plugins.unrealengine.common.buildgraph.BuildGraphSettings
import com.jetbrains.teamcity.plugins.unrealengine.common.ensureNotNull
import jetbrains.buildServer.serverSide.artifacts.BuildArtifact
import jetbrains.buildServer.serverSide.artifacts.BuildArtifacts
import jetbrains.buildServer.serverSide.artifacts.BuildArtifactsViewMode

class BuildGraphDefinitionLoader(
    private val parser: BuildGraphParser,
    private val settings: BuildGraphSettings,
) {
    context(_: Raise<Error>)
    fun loadFrom(setupBuild: ValidatedSetupBuild): BuildGraph<BuildGraphNodeGroup> {
        val buildGraphFile =
            ensureNotNull(
                findPublishedGraph(setupBuild),
                "It appears that the build graph setup build has failed, it hasn't published the exported graph file",
            )

        return parser.parse(buildGraphFile.inputStream)
    }

    private fun findPublishedGraph(setupBuild: ValidatedSetupBuild): BuildArtifact? {
        var result: BuildArtifact? = null

        setupBuild.getArtifacts(BuildArtifactsViewMode.VIEW_DEFAULT).iterateArtifacts {
            if (it.name == settings.graphArtifactName && it.isFile) {
                result = it
                BuildArtifacts.BuildArtifactsProcessor.Continuation.BREAK
            } else {
                BuildArtifacts.BuildArtifactsProcessor.Continuation.CONTINUE
            }
        }

        return result
    }
}
