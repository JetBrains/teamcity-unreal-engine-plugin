package com.jetbrains.teamcity.plugins.unrealengine.server.build.state

import arrow.core.raise.Raise
import com.jetbrains.teamcity.plugins.unrealengine.common.Error
import com.jetbrains.teamcity.plugins.unrealengine.common.ensureNotNull
import com.jetbrains.teamcity.plugins.unrealengine.server.build.getUnrealDataStorage
import jetbrains.buildServer.serverSide.CustomDataStorage
import jetbrains.buildServer.serverSide.SBuild
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.properties.Properties
import kotlinx.serialization.properties.decodeFromStringMap
import kotlinx.serialization.properties.encodeToStringMap

@OptIn(ExperimentalSerializationApi::class)
class DistributedBuildStateStorage {
    private val properties = Properties

    fun init(
        parentBuild: SBuild,
        state: DistributedBuildState,
    ) {
        parentBuild.buildStateStorage.putValues(
            properties.encodeToStringMap(state),
        )
    }

    context(_: Raise<Error>)
    fun get(build: SBuild): DistributedBuildState {
        val stateMap = ensureNotNull(build.buildStateStorage.values, "There is no state associated with this build")
        return properties.decodeFromStringMap<DistributedBuildState>(stateMap)
    }

    context(_: Raise<Error>)
    fun update(
        build: SBuild,
        steps: Sequence<DistributedBuildState.BuildStep>,
    ): DistributedBuildState {
        val state = get(build)

        val propertiesToUpdate =
            steps
                .flatMap {
                    val (buildIndex, stepIndex) =
                        state
                            .locateBuildStep(it.name)
                            .let { location ->
                                ensureNotNull(location, "There is no step named ${it.name} within a build ${build.buildId}")
                            }

                    properties
                        .encodeToStringMap(it)
                        .mapKeys { (key, _) -> formatStepKeyWithinBuild(key, buildIndex, stepIndex) }
                        .entries
                }.associate { it.toPair() }

        build.buildStateStorage.updateValues(propertiesToUpdate, emptySet())
        return get(build)
    }

    fun dispose(build: SBuild) = build.buildStateStorage.dispose()

    private val SBuild.buildStateStorage: CustomDataStorage
        get() = getUnrealDataStorage("distributed-build-state")

    private fun formatStepKeyWithinBuild(
        key: String,
        build: Int,
        step: Int,
    ): String = "builds.$build.steps.$step.$key"

    private fun DistributedBuildState.locateBuildStep(name: String): Pair<Int, Int>? =
        builds.withIndex().firstNotNullOfOrNull { (buildIndex, build) ->
            build.steps
                .withIndex()
                .firstOrNull { (_, step) -> step.name == name }
                ?.let { (stepIndex, _) ->
                    buildIndex to stepIndex
                }
        }
}
