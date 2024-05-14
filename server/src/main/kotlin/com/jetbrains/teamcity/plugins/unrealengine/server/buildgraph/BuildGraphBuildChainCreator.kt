package com.jetbrains.teamcity.plugins.unrealengine.server.buildgraph

import arrow.core.Either
import arrow.core.raise.either
import com.jetbrains.teamcity.plugins.framework.common.TeamCityLoggers
import com.jetbrains.teamcity.plugins.unrealengine.common.UnrealEngineRunner
import com.jetbrains.teamcity.plugins.unrealengine.common.buildgraph.BuildGraphRunnerInternalSettings
import com.jetbrains.teamcity.plugins.unrealengine.common.buildgraph.toMap
import jetbrains.buildServer.requirements.Requirement
import jetbrains.buildServer.requirements.RequirementType
import jetbrains.buildServer.serverSide.BuildPromotionEx
import jetbrains.buildServer.serverSide.BuildQueueEx
import jetbrains.buildServer.serverSide.BuildServerAdapter
import jetbrains.buildServer.serverSide.SAgentRestrictor
import jetbrains.buildServer.serverSide.SRunningBuild
import jetbrains.buildServer.serverSide.SimpleParameter
import jetbrains.buildServer.serverSide.artifacts.BuildArtifact
import jetbrains.buildServer.serverSide.artifacts.BuildArtifacts
import jetbrains.buildServer.serverSide.artifacts.BuildArtifactsViewMode
import jetbrains.buildServer.util.DependencyOptionSupportImpl
import jetbrains.buildServer.virtualConfiguration.generator.VirtualBuildTypeSettings
import jetbrains.buildServer.virtualConfiguration.generator.VirtualPromotionGeneratorFactory
import java.io.InputStream

class BuildGraphBuildChainCreator(
    private val buildGraphParser: BuildGraphParser,
    private val buildGeneratorFactory: VirtualPromotionGeneratorFactory,
    private val buildQueue: BuildQueueEx,
    private val settings: BuildGraphSettings,
) : BuildServerAdapter() {

    companion object {
        private val logger = TeamCityLoggers.server<BuildGraphBuildChainCreator>()
        private val restrictedIdCharactersRegex = "[^A-Za-z0-9_]".toRegex()
    }

    override fun beforeBuildFinish(runningBuild: SRunningBuild) {
        val buildType = runningBuild.buildType
        if (buildType == null) {
            logger.debug("The running build \"${runningBuild.fullName}\" is missing a build type, skipping")
            return
        }

        val isBuildGraphSetup = buildType.isVirtual &&
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

    private fun processExportedBuildGraph(buildGraph: InputStream, runningBuild: SRunningBuild) {
        val parseResult = either { buildGraphParser.parse(buildGraph) }
        when (parseResult) {
            is Either.Left -> {
                logger.error("An error occurred while parsing exported build graph file", parseResult.value.exception)
                throw parseResult.value.exception
            }
            is Either.Right -> runCatching {
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

    private fun insertBuildGraph(setupBuild: BuildPromotionEx, buildGraph: BuildGraph<BuildGraphNodeGroup>) {
        val originalBuild = setupBuild.dependedOnMe.single().dependent
        val buildChain = createChainFromGraph(buildGraph, originalBuild)

        setupBuild.addAsADependencyTo(buildChain.starts)
        originalBuild.addDependencies(buildChain.ends)
        originalBuild.removeDependency(setupBuild)

        buildQueue.addToQueue(buildChain.builds.toMapWithoutAgentRestrictions(), originalBuild.asTriggeredBy())

        originalBuild.persist()
        setupBuild.persist()
    }

    private fun List<BuildPromotionEx>.toMapWithoutAgentRestrictions(): Map<BuildPromotionEx, SAgentRestrictor?> =
        associateWith { null }

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
        val runnerParameters = originalBuild.activeRunners().last().parameters

        val buildsToAdd = buildGraph.topologicalSort()
            .map {
                val build = createBuildForGroupOfNodes(it, runnerParameters, originalBuild)

                for (successor in buildGraph.adjacencyList[it]!!) {
                    groupDependencies.computeIfAbsent(successor) { mutableListOf() }
                    groupDependencies[successor]!!.add(build)
                }

                build.setRevisionsFrom(originalBuild)

                build.addDependencies(groupDependencies[it].orEmpty())

                build
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

    private fun createBuildForGroupOfNodes(
        group: BuildGraphNodeGroup,
        runnerParameters: Map<String, String>,
        originalBuild: BuildPromotionEx,
    ): BuildPromotionEx {
        return buildGeneratorFactory.create(originalBuild).getOrCreate(
            VirtualBuildTypeSettings(
                originalBuild.generateIdForVirtualBuild(group.name).toExternalId(),
                group.name,
            ),
        ) { buildConfiguration, _ ->
            for (node in group.nodes) {
                buildConfiguration.addBuildRunner(
                    node.name,
                    UnrealEngineRunner.RUN_TYPE,
                    runnerParameters +
                        mapOf(
                            "additional-arguments" to
                                """
                                        "-SingleNode=${node.name}"
                                        -utf8output -buildmachine -unattended -noP4 -nosplash -stdout -NoCodeSign
                                """.trimIndent(),
                        ) +
                        BuildGraphRunnerInternalSettings.RegularBuildSettings(
                            originalBuild.id.toString(),
                        ).toMap(),
                )
            }

            if (group.agents.any()) {
                buildConfiguration.addRequirement(
                    Requirement(
                        "unreal-engine.build-graph.agent.type",
                        ".*(;|^)(${group.agents.joinToString(separator = "|")})(;|$).*",
                        RequirementType.MATCHES,
                    ),
                )
            }

            originalBuild.buildParameters.forEach { (name, value) ->
                buildConfiguration.addParameter(SimpleParameter(name, value))
            }

            val changed = true
            changed
        } as BuildPromotionEx
    }

    private fun String.toExternalId() = restrictedIdCharactersRegex.replace(this, "_")
}
