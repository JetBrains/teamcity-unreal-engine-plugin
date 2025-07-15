package com.jetbrains.teamcity.plugins.unrealengine.server.discovery

import com.jetbrains.teamcity.plugins.unrealengine.common.UnrealEngineProject
import com.jetbrains.teamcity.plugins.unrealengine.common.UnrealEngineRunner
import com.jetbrains.teamcity.plugins.unrealengine.common.UnrealPluginLoggers
import com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun.BuildCookRunProjectPathParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun.UnrealTargetPlatformsParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.parameters.UnrealEngineIdentifierParameter
import com.jetbrains.teamcity.plugins.unrealengine.server.runner.UnrealEngineRunnerParametersProvider
import jetbrains.buildServer.serverSide.discovery.BreadthFirstRunnerDiscoveryExtension
import jetbrains.buildServer.serverSide.discovery.DiscoveredObject
import jetbrains.buildServer.util.browser.Element

class UnrealEngineRunnerDiscoverer(
    private val unrealProjectDiscoverers: Collection<UnrealProjectDiscoverer>,
    defaultPropertiesProvider: UnrealEngineRunnerParametersProvider,
) : BreadthFirstRunnerDiscoveryExtension() {
    private val defaultRunnerParameters = defaultPropertiesProvider.getDefaultValues()

    companion object {
        private val logger = UnrealPluginLoggers.get<UnrealEngineRunnerDiscoverer>()
    }

    override fun discoverRunnersInDirectory(
        dir: Element,
        filesAndDirs: MutableList<Element>,
    ): MutableList<DiscoveredObject> {
        val unrealProjects =
            unrealProjectDiscoverers
                .flatMap {
                    it
                        .runCatching { discover(dir) }
                        .getOrElse { error ->
                            logger.error(
                                """
                                An error occurred during unreal engine project discovery.
                                Discoverer: ${it.javaClass.canonicalName} Error: $error
                                """.trimIndent(),
                            )
                            emptyList()
                        }
                }.distinct()

        logger.debug("Number of found unreal engine projects in ${dir.fullName}: ${unrealProjects.size}")

        val discovered =
            unrealProjects
                .map { DiscoveredObject(UnrealEngineRunner.RUN_TYPE, defaultRunnerParameters + it.toDiscoveredProperties()) }
                .distinctBy { it.parameters }

        logger.debug("Total number of discovered runners to suggest: ${discovered.size}")

        return discovered.toMutableList()
    }

    private fun UnrealEngineProject.toDiscoveredProperties() =
        buildMap {
            put(BuildCookRunProjectPathParameter.name, location.value)
            engineIdentifier?.let { put(UnrealEngineIdentifierParameter.name, it.value) }

            if (targetPlatforms.any()) {
                putAll(
                    sequenceOf(
                        UnrealTargetPlatformsParameter.Standalone,
                        UnrealTargetPlatformsParameter.Client,
                        UnrealTargetPlatformsParameter.Server,
                    ).map {
                        it.name to UnrealTargetPlatformsParameter.joinPlatforms(targetPlatforms)
                    },
                )
            }
        }
}
