package com.jetbrains.teamcity.plugins.unrealengine.server.buildgraph

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import com.jetbrains.teamcity.plugins.framework.common.TeamCityLoggers
import com.jetbrains.teamcity.plugins.unrealengine.common.UnrealEngineRunner
import com.jetbrains.teamcity.plugins.unrealengine.common.buildgraph.BuildGraphRunnerInternalSettings
import com.jetbrains.teamcity.plugins.unrealengine.common.buildgraph.toMap
import com.jetbrains.teamcity.plugins.unrealengine.common.parameters.AdditionalArgumentsParameter
import jetbrains.buildServer.agent.AgentRuntimeProperties
import jetbrains.buildServer.serverSide.BuildAttributes
import jetbrains.buildServer.serverSide.BuildPromotion
import jetbrains.buildServer.serverSide.BuildPromotionEx
import jetbrains.buildServer.serverSide.BuildQueueEx
import jetbrains.buildServer.serverSide.impl.LogUtil
import jetbrains.buildServer.util.DependencyOptionSupportImpl
import jetbrains.buildServer.virtualConfiguration.generator.VirtualBuildTypeSettings
import jetbrains.buildServer.virtualConfiguration.generator.VirtualPromotionGeneratorFactory
import jetbrains.buildServer.virtualConfiguration.processor.ProcessVirtualConfigurations

class BuildGraphDistributionConfigurer(
    private val buildQueue: BuildQueueEx,
    private val buildGeneratorFactory: VirtualPromotionGeneratorFactory,
    private val settings: BuildGraphSettings,
) : ProcessVirtualConfigurations {

    companion object {
        private val logger = TeamCityLoggers.server<BuildGraphDistributionConfigurer>()
    }

    override fun addToQueue(build: BuildPromotion): MutableList<BuildPromotion> {
        if (shouldDistributeBuild((build as BuildPromotionEx))) {
            // Since changes haven't been collected at this moment, we aren't creating anything and are merely
            // indicating that this is required for the later split stage (freeze)
            build.ensureChangeCollectionWhileInQueue()
        }

        return mutableListOf()
    }

    private fun BuildPromotionEx.ensureChangeCollectionWhileInQueue() =
        setAttribute(BuildAttributes.FREEZE_REQUIRES_COLLECTED_CHANGES, true.toString())

    override fun freeze(build: BuildPromotion): MutableList<BuildPromotion> = runCatching {
        distributeBuild(build as BuildPromotionEx).toMutableList()
    }
        .getOrElse {
            logger.error("An error occurred while trying to set up BuildGraph build distribution", it)
            mutableListOf()
        }

    private fun distributeBuild(build: BuildPromotionEx) = sequence<BuildPromotion> {
        if (!shouldDistributeBuild(build)) {
            logger.debug(
                """
                Build won't be distributed because it doesn't satisfy some of the distribution criteria.
                It is either already virtual, doesn't contain a single active Unreal build step,
                or doesn't have any VCS roots attached
                """.trimIndent(),
            )
            return@sequence
        }

        logger.info(
            "Build graph build ${build.toLogString()} is " +
                "eligible for distribution across multiple machines",
        )

        when (val result = setupBuildDistribution(build)) {
            is Either.Left -> {
                logger.error(
                    "An error occurred while setting up a distribution of the build " +
                        "${build.toLogString()}. Proceeding with the default setup",
                )
                return@sequence
            }
            is Either.Right -> {
                yield(result.value)
            }
        }
    }

    override fun getType() = "UnrealEngine_BuildGraph"

    private fun shouldDistributeBuild(build: BuildPromotion): Boolean {
        val isVirtual = build.buildType?.isVirtual ?: false
        val isDistributedBuildGraph = build.hasSingleDistributedBuildGraphStep()

        return build.vcsRootEntries.isNotEmpty() && !isVirtual && isDistributedBuildGraph
    }

    private fun setupBuildDistribution(
        originalBuild: BuildPromotionEx,
    ): Either<BuildGraphConfigurationError, BuildPromotionEx> = either {
        val originalBuildParentProjectId = originalBuild.projectId
        ensure(originalBuildParentProjectId != null) {
            logger.debug("The build ${originalBuild.toLogString()} has no parent project, skipping")
            BuildGraphConfigurationError("Build is missing its parent project")
        }

        originalBuild.markAsComposite()
        val setupBuild = createBuildGraphSetupBuild(originalBuild)
        originalBuild.persist()

        buildQueue.addToQueue(mapOf(setupBuild to null), originalBuild.asTriggeredBy())

        logger.info(
            "Build ${originalBuild.toLogString()} has been successfully converted into a " +
                "composite build with a graph generation setup build",
        )

        setupBuild
    }

    private fun BuildPromotionEx.markAsComposite() = setAttribute(BuildAttributes.COMPOSITE_BUILD, true.toString())

    private fun createBuildGraphSetupBuild(originalBuild: BuildPromotionEx): BuildPromotionEx {
        val originalRunnerParameters = originalBuild.activeRunners().single().parameters

        val buildGenerator = buildGeneratorFactory.create(originalBuild)
        val setupBuild = buildGenerator.getOrCreate(
            VirtualBuildTypeSettings(
                originalBuild.generateIdForVirtualBuild(settings.setupBuildName),
                settings.setupBuildName,
            ),
        ) { buildConfiguration, _ ->
            val graphExportPath =
                "%${AgentRuntimeProperties.BUILD_CHECKOUT_DIR}%/${settings.graphArtifactName}"

            buildConfiguration.addBuildRunner(
                settings.setupBuildName,
                UnrealEngineRunner.RUN_TYPE,
                originalRunnerParameters +
                    mapOf(
                        AdditionalArgumentsParameter.name to
                            """
                                        "-Export=$graphExportPath"
                                        -utf8output -buildmachine -unattended -noP4 -nosplash -stdout -NoCodeSign
                            """.trimIndent(),
                    ) + BuildGraphRunnerInternalSettings.SetupBuildSettings(
                        graphExportPath,
                        originalBuild.id.toString(),
                    ).toMap(),
            )

            val changed = true
            changed
        }

        (setupBuild as BuildPromotionEx).setRevisionsFrom(originalBuild)
        val dependencyOptions = DependencyOptionSupportImpl().default()
        originalBuild.addDependency(setupBuild, dependencyOptions)

        return setupBuild
    }

    private fun BuildPromotionEx.toLogString(): String = LogUtil.describe(this)
}
