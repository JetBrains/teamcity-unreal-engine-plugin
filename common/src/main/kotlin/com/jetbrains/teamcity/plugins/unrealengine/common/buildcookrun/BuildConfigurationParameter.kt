package com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun

import arrow.core.NonEmptyList
import arrow.core.nonEmptyListOf
import arrow.core.raise.Raise
import com.jetbrains.teamcity.plugins.framework.common.raise
import com.jetbrains.teamcity.plugins.framework.common.zipOrAccumulate
import com.jetbrains.teamcity.plugins.unrealengine.common.PropertyValidationError
import com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun.UnrealBuildTargetParameter.parseBuildTargets
import com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun.UnrealTargetConfigurationsParameter.parseTargetConfigurations
import com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun.UnrealTargetPlatformsParameter.parseTargetPlatforms
import com.jetbrains.teamcity.plugins.unrealengine.common.parameters.SelectOption
import com.jetbrains.teamcity.plugins.unrealengine.common.parameters.SelectParameter

object BuildConfigurationParameter : SelectParameter() {
    @Suppress("MayBeConstant", "RedundantSuppression") // constants aren't accessible from JSP
    val standalone = SelectOption("StandaloneGame", "Standalone game")

    @Suppress("MayBeConstant", "RedundantSuppression") // constants aren't accessible from JSP
    val client = SelectOption("Client")

    @Suppress("MayBeConstant", "RedundantSuppression") // constants aren't accessible from JSP
    val server = SelectOption("Server")

    @Suppress("MayBeConstant", "RedundantSuppression") // constants aren't accessible from JSP
    val clientAndServer = SelectOption("ClientAndServer", "Client and server")

    override val name = "build-cook-run-build-type"
    override val displayName = "Build configuration"
    override val defaultValue = standalone.name
    override val description = null

    override val options =
        listOf(
            standalone,
            client,
            server,
            clientAndServer,
        )

    context(_: Raise<NonEmptyList<PropertyValidationError>>)
    fun parseBuildConfiguration(runnerParameters: Map<String, String>) =
        when (runnerParameters[name]) {
            standalone.name -> parseStandalone(runnerParameters)
            client.name -> parseClient(runnerParameters)
            server.name -> parseServer(runnerParameters)
            clientAndServer.name -> parseClientAndServer(runnerParameters)
            else -> {
                raise(nonEmptyListOf(PropertyValidationError(name, "Unknown build configuration type.")))
            }
        }

    context(_: Raise<NonEmptyList<PropertyValidationError>>)
    private fun parseStandalone(runnerParameters: Map<String, String>) =
        zipOrAccumulate(
            { parseTargetConfigurations(runnerParameters, UnrealTargetConfigurationsParameter.Standalone.name) },
            { parseTargetPlatforms(runnerParameters, UnrealTargetPlatformsParameter.Standalone.name) },
        ) { configuration, platforms ->
            BuildConfiguration.Standalone(
                configuration,
                platforms,
                parseBuildTargets(runnerParameters),
            )
        }

    context(_: Raise<NonEmptyList<PropertyValidationError>>)
    private fun parseClient(runnerParameters: Map<String, String>) =
        zipOrAccumulate(
            { parseTargetConfigurations(runnerParameters, UnrealTargetConfigurationsParameter.Client.name) },
            { parseTargetPlatforms(runnerParameters, UnrealTargetPlatformsParameter.Client.name) },
        ) { configuration, platforms -> BuildConfiguration.Client(configuration, platforms, parseBuildTargets(runnerParameters)) }

    context(_: Raise<NonEmptyList<PropertyValidationError>>)
    private fun parseServer(runnerParameters: Map<String, String>) =
        zipOrAccumulate(
            { parseTargetConfigurations(runnerParameters, UnrealTargetConfigurationsParameter.Server.name) },
            { parseTargetPlatforms(runnerParameters, UnrealTargetPlatformsParameter.Server.name) },
        ) { configuration, platforms -> BuildConfiguration.Server(configuration, platforms, parseBuildTargets(runnerParameters)) }

    context(_: Raise<NonEmptyList<PropertyValidationError>>)
    private fun parseClientAndServer(runnerParameters: Map<String, String>) =
        zipOrAccumulate(
            { parseClient(runnerParameters) },
            { parseServer(runnerParameters) },
        ) { client, server ->
            BuildConfiguration.ClientAndServer(
                client.configuration,
                client.platforms,
                server.configuration,
                server.platforms,
                parseBuildTargets(runnerParameters),
            )
        }
}
