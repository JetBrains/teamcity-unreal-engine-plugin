package com.jetbrains.teamcity.plugins.unrealengine.server.buildgraph

import arrow.core.Either
import arrow.core.raise.either
import com.jetbrains.teamcity.plugins.framework.common.TeamCityLoggers
import com.jetbrains.teamcity.plugins.unrealengine.common.buildgraph.BuildGraphRunnerInternalSettings
import com.jetbrains.teamcity.plugins.unrealengine.common.buildgraph.toMap
import com.jetbrains.teamcity.plugins.unrealengine.common.parameters.AdditionalArgumentsParameter
import jetbrains.buildServer.requirements.Requirement
import jetbrains.buildServer.requirements.RequirementType
import jetbrains.buildServer.serverSide.BuildPromotionEx
import jetbrains.buildServer.serverSide.BuildQueueEx
import jetbrains.buildServer.serverSide.BuildServerAdapter
import jetbrains.buildServer.serverSide.SAgentRestrictor
import jetbrains.buildServer.serverSide.SRunningBuild
import jetbrains.buildServer.serverSide.artifacts.BuildArtifact
import jetbrains.buildServer.serverSide.artifacts.BuildArtifacts
import jetbrains.buildServer.serverSide.artifacts.BuildArtifactsViewMode
import jetbrains.buildServer.util.DependencyOptionSupportImpl
import java.io.InputStream

class BuildGraphBuildChainCreator(
    private val buildGraphParser: BuildGraphParser,
    private val virtualBuildCreator: BuildGraphVirtualBuildCreator,
    private val buildQueue: BuildQueueEx,
    private val settings: BuildGraphSettings,
) : BuildServerAdapter() {
    companion object {
        private val logger = TeamCityLoggers.server<BuildGraphBuildChainCreator>()
    }

    override fun beforeBuildFinish(runningBuild: SRunningBuild) {
        val buildType = runningBuild.buildType
        if (buildType == null) {
            logger.debug("The running build \"${runningBuild.fullName}\" is missing a build type, skipping")
            return
        }

        val isBuildGraphSetup =
            buildType.isVirtual &&
                runningBuild.buildPromotion.hasSingleDistributedBuildGraphStep() &&
                buildType.name == settings.setupBuildName

        if (!isBuildGraphSetup) {
            logger.debug("The running build \"${runningBuild.fullName}\" isn't a build graph setup build, skipping")
            return
        }

        if (runningBuild.projectId == null) {
            logger.warn("Build graph setup build is missing project id. Build chain won't be created")
            return
        }

        var artifactWasPublished = false
        runningBuild.getArtifacts(BuildArtifactsViewMode.VIEW_DEFAULT).iterateArtifacts { artifact ->
            if (artifact.isExportedBuildGraph()) {
                artifactWasPublished = true
                logger.debug("Found published build graph artifact, proceeding to parse and process it")
                processExportedBuildGraph(artifact.inputStream, runningBuild)
                BuildArtifacts.BuildArtifactsProcessor.Continuation.BREAK
            } else {
                BuildArtifacts.BuildArtifactsProcessor.Continuation.CONTINUE
            }
        }

        if (!artifactWasPublished) {
            logger.warn("It appears that the build graph setup build has failed, it hasn't published the exported graph file")
        }
    }

    private fun processExportedBuildGraph(
        buildGraph: InputStream,
        runningBuild: SRunningBuild,
    ) {
        val parseResult = either { buildGraphParser.parse(buildGraph) }
        when (parseResult) {
            is Either.Left -> {
                logger.error("An error occurred while parsing exported build graph file", parseResult.value.exception)
                throw parseResult.value.exception
            }
            is Either.Right ->
                runCatching {
                    insertBuildGraph(
                        (runningBuild.buildPromotion as BuildPromotionEx),
                        parseResult.value,
                    )
                }.getOrElse {
                    logger.error("An error occurred while trying to insert parsed BuildGraph into original build", it)
                    throw it
                }
        }
    }

    private fun BuildArtifact.isExportedBuildGraph() = name == settings.graphArtifactName && isFile

    private fun insertBuildGraph(
        setupBuild: BuildPromotionEx,
        buildGraph: BuildGraph<BuildGraphNodeGroup>,
    ) {
        val originalBuild = setupBuild.dependedOnMe.single().dependent
        val buildChain = createChainFromGraph(buildGraph, originalBuild)

        setupBuild.addAsADependencyTo(buildChain.starts)
        originalBuild.addDependencies(buildChain.ends)
        originalBuild.removeDependency(setupBuild)

        buildQueue.addToQueue(buildChain.builds.toMapWithoutAgentRestrictions(), originalBuild.asTriggeredBy())

        originalBuild.persist()
        setupBuild.persist()
    }

    private fun List<BuildPromotionEx>.toMapWithoutAgentRestrictions(): Map<BuildPromotionEx, SAgentRestrictor?> = associateWith { null }

    private data class BuildChain(
        val builds: List<BuildPromotionEx>,
    ) {
        val starts = builds.filter { it.dependencies.isEmpty() }
        val ends = builds.filter { it.dependedOnMe.isEmpty() }
    }

    private fun createChainFromGraph(
        buildGraph: BuildGraph<BuildGraphNodeGroup>,
        originalBuild: BuildPromotionEx,
    ): BuildChain {
        val groupDependencies = mutableMapOf<BuildGraphNodeGroup, MutableList<BuildPromotionEx>>()

        val buildsToAdd =
            with(virtualBuildCreator.inContextOf(originalBuild)) {
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
            }

        return BuildChain(buildsToAdd)
    }

    private fun BuildPromotionEx.addDependencies(dependencies: Collection<BuildPromotionEx>) {
        val options = DependencyOptionSupportImpl().default()
        for (dependency in dependencies) {
            addDependency(dependency, options)
        }
    }

    private fun BuildPromotionEx.addAsADependencyTo(dependents: Collection<BuildPromotionEx>) {
        val dependencyOptions = DependencyOptionSupportImpl().default()
        for (dependent in dependents) {
            dependent.addDependency(this, dependencyOptions)
        }
    }

    context(BuildGraphVirtualBuildCreator.VirtualBuildCreationContext)
    private fun createBuildForGroupOfNodes(group: BuildGraphNodeGroup): BuildPromotionEx {
        val originalRunnerParameters = originalBuild.activeRunners().single().parameters
        val originalBuildId = originalBuild.id.toString()

        return virtualBuildCreator.create(group.name) {
            for (node in group.nodes) {
                val parameters =
                    originalRunnerParameters +
                        mapOf(
                            AdditionalArgumentsParameter.name to
                                originalRunnerParameters[AdditionalArgumentsParameter.name] + " \"-SingleNode=${node.name}\"",
                        ) +
                        BuildGraphRunnerInternalSettings
                            .RegularBuildSettings(
                                originalBuildId,
                            ).toMap()

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
}
