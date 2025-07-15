package com.jetbrains.teamcity.plugins.unrealengine.server.buildgraph

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import com.jetbrains.teamcity.plugins.unrealengine.common.GenericError
import com.jetbrains.teamcity.plugins.unrealengine.common.UnrealPluginLoggers
import com.jetbrains.teamcity.plugins.unrealengine.common.buildgraph.BuildGraphRunnerInternalSettings
import com.jetbrains.teamcity.plugins.unrealengine.common.buildgraph.BuildGraphSettings
import com.jetbrains.teamcity.plugins.unrealengine.common.buildgraph.toMap
import com.jetbrains.teamcity.plugins.unrealengine.common.parameters.AdditionalArgumentsParameter
import com.jetbrains.teamcity.plugins.unrealengine.server.extensions.activeRunners
import com.jetbrains.teamcity.plugins.unrealengine.server.extensions.addUnrealRunner
import com.jetbrains.teamcity.plugins.unrealengine.server.extensions.asBuildPromotionEx
import com.jetbrains.teamcity.plugins.unrealengine.server.extensions.asTriggeredBy
import com.jetbrains.teamcity.plugins.unrealengine.server.extensions.default
import com.jetbrains.teamcity.plugins.unrealengine.server.extensions.hasSingleDistributedBuildGraphStep
import jetbrains.buildServer.agent.AgentRuntimeProperties
import jetbrains.buildServer.serverSide.BuildAttributes
import jetbrains.buildServer.serverSide.BuildPromotion
import jetbrains.buildServer.serverSide.BuildPromotionEx
import jetbrains.buildServer.serverSide.BuildQueueEx
import jetbrains.buildServer.serverSide.impl.LogUtil
import jetbrains.buildServer.util.DependencyOptionSupportImpl
import jetbrains.buildServer.virtualConfiguration.processor.ProcessVirtualConfigurations

class BuildGraphDistributionConfigurer(
    private val buildQueue: BuildQueueEx,
    private val virtualBuildCreator: BuildGraphVirtualBuildCreator,
    private val settings: BuildGraphSettings,
) : ProcessVirtualConfigurations {
    companion object {
        private val logger = UnrealPluginLoggers.get<BuildGraphDistributionConfigurer>()
    }

    override fun addToQueue(build: BuildPromotion): MutableList<BuildPromotion> =
        build
            .asBuildPromotionEx()
            .let {
                if (shouldDistributeBuild(it)) {
                    it.ensureChangeCollectionWhileInQueue()
                }
                mutableListOf()
            }

    private fun BuildPromotionEx.ensureChangeCollectionWhileInQueue() =
        setAttribute(BuildAttributes.FREEZE_REQUIRES_COLLECTED_CHANGES, true.toString())

    override fun freeze(build: BuildPromotion): MutableList<BuildPromotion> =
        runCatching {
            distributeBuild(build.asBuildPromotionEx()).toMutableList()
        }.getOrElse {
            logger.error("An error occurred while trying to set up BuildGraph build distribution", it)
            mutableListOf()
        }

    private fun distributeBuild(build: BuildPromotionEx) =
        sequence<BuildPromotion> {
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

    private fun shouldDistributeBuild(build: BuildPromotionEx): Boolean {
        val isVirtual = build.buildType?.isVirtual ?: false
        val isDistributedBuildGraph = build.hasSingleDistributedBuildGraphStep()

        return build.vcsRootEntries.isNotEmpty() && !isVirtual && isDistributedBuildGraph
    }

    private fun setupBuildDistribution(originalBuild: BuildPromotionEx): Either<GenericError, BuildPromotionEx> =
        either {
            val originalBuildParentProjectId = originalBuild.projectId
            ensure(originalBuildParentProjectId != null) {
                logger.debug("The build ${originalBuild.toLogString()} has no parent project, skipping")
                raise(GenericError("Build is missing its parent project"))
            }

            originalBuild.markAsComposite()
            val setupBuild = createBuildGraphSetupBuild(originalBuild)
            originalBuild.setAttribute(settings.buildGraphGeneratedMarker, true)
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

        val setupBuild =
            with(virtualBuildCreator.inContextOf(originalBuild)) {
                virtualBuildCreator.create(settings.setupBuildName) {
                    val graphExportPath =
                        "%${AgentRuntimeProperties.BUILD_CHECKOUT_DIR}%/${settings.graphArtifactName}"

                    val setupRunnerParameters =
                        originalRunnerParameters +
                            mapOf(
                                AdditionalArgumentsParameter.name to
                                    originalRunnerParameters[AdditionalArgumentsParameter.name] + " \"-Export=$graphExportPath\"",
                            ) +
                            BuildGraphRunnerInternalSettings
                                .SetupBuildSettings(
                                    graphExportPath,
                                    originalBuild.id.toString(),
                                ).toMap()

                    addUnrealRunner(
                        settings.setupBuildName,
                        setupRunnerParameters,
                    )
                }
            }

        val dependencyOptions = DependencyOptionSupportImpl().default()
        originalBuild.addDependency(setupBuild, dependencyOptions)

        return setupBuild
    }

    private fun BuildPromotionEx.toLogString(): String = LogUtil.describe(this)
}
