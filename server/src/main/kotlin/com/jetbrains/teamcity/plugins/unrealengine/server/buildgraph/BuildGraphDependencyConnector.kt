package com.jetbrains.teamcity.plugins.unrealengine.server.buildgraph

import com.jetbrains.teamcity.plugins.unrealengine.server.build.DistributedBuild
import com.jetbrains.teamcity.plugins.unrealengine.server.extensions.addDependencies
import com.jetbrains.teamcity.plugins.unrealengine.server.extensions.asBuildPromotionEx
import com.jetbrains.teamcity.plugins.unrealengine.server.extensions.default
import jetbrains.buildServer.artifacts.RevisionRules
import jetbrains.buildServer.serverSide.ArtifactDependencyFactory
import jetbrains.buildServer.serverSide.BuildPromotionEx
import jetbrains.buildServer.serverSide.SBuild
import jetbrains.buildServer.serverSide.dependency.DependencyOptions
import jetbrains.buildServer.util.DependencyOptionSupportImpl

class BuildGraphDependencyConnector(
    private val artifactDependencyFactory: ArtifactDependencyFactory,
) {
    fun connect(
        setupBuild: ValidatedSetupBuild,
        generatedBuild: DistributedBuild,
        originalBuild: SBuild,
    ) {
        val setupBuildPromotion = setupBuild.buildPromotion.asBuildPromotionEx()
        val originalBuildPromotion = originalBuild.buildPromotion.asBuildPromotionEx()

        setupBuildPromotion.addAsADependencyTo(generatedBuild.starts)

        with(originalBuildPromotion) {
            addDependencies(generatedBuild.ends) {
                setOption(DependencyOptions.RUN_BUILD_IF_DEPENDENCY_FAILED, DependencyOptions.BuildContinuationMode.RUN_ADD_PROBLEM)
                setOption(
                    DependencyOptions.RUN_BUILD_IF_DEPENDENCY_FAILED_TO_START,
                    DependencyOptions.BuildContinuationMode.RUN_ADD_PROBLEM,
                )
            }

            customArtifactDependencies =
                artifactDependencies +
                generatedBuild.builds.map {
                    artifactDependencyFactory.createArtifactDependency(
                        it.buildTypeExternalId,
                        "**/* => .",
                        RevisionRules.LAST_FINISHED_SAME_CHAIN_RULE,
                    )
                }

            removeDependency(setupBuildPromotion)
            persist()
        }

        setupBuildPromotion.persist()
    }

    private fun BuildPromotionEx.addAsADependencyTo(dependents: Collection<BuildPromotionEx>) {
        val dependencyOptions = DependencyOptionSupportImpl().default()
        for (dependent in dependents) {
            dependent.addDependency(this, dependencyOptions)
        }
    }
}
