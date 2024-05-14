package com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun

import arrow.core.NonEmptyList
import arrow.core.nonEmptyListOf
import arrow.core.raise.Raise
import arrow.core.raise.zipOrAccumulate
import com.jetbrains.teamcity.plugins.unrealengine.common.ValidationError
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

    override val options = listOf(
        standalone,
        client,
        server,
        clientAndServer,
    )

    context(Raise<NonEmptyList<ValidationError>>)
    fun parseBuildConfiguration(properties: Map<String, String>) = when (properties[name]) {
        standalone.name -> parseStandalone(properties)
        client.name -> parseClient(properties)
        server.name -> parseServer(properties)
        clientAndServer.name -> parseClientAndServer(properties)
        else -> {
            raise(nonEmptyListOf(ValidationError(name, "Unknown build configuration type.")))
        }
    }

    context(Raise<NonEmptyList<ValidationError>>)
    private fun parseStandalone(properties: Map<String, String>) = zipOrAccumulate(
        { parseTargetConfigurations(properties, UnrealTargetConfigurationsParameter.Standalone.name) },
        { parseTargetPlatforms(properties, UnrealTargetPlatformsParameter.Standalone.name) },
    ) { configuration, platforms -> BuildConfiguration.Standalone(configuration, platforms, parseBuildTargets(properties)) }

    context(Raise<NonEmptyList<ValidationError>>)
    private fun parseClient(properties: Map<String, String>) = zipOrAccumulate(
        { parseTargetConfigurations(properties, UnrealTargetConfigurationsParameter.Client.name) },
        { parseTargetPlatforms(properties, UnrealTargetPlatformsParameter.Client.name) },
    ) { configuration, platforms -> BuildConfiguration.Client(configuration, platforms, parseBuildTargets(properties)) }

    context(Raise<NonEmptyList<ValidationError>>)
    private fun parseServer(properties: Map<String, String>) = zipOrAccumulate(
        { parseTargetConfigurations(properties, UnrealTargetConfigurationsParameter.Server.name) },
        { parseTargetPlatforms(properties, UnrealTargetPlatformsParameter.Server.name) },
    ) { configuration, platforms -> BuildConfiguration.Server(configuration, platforms, parseBuildTargets(properties)) }

    context(Raise<NonEmptyList<ValidationError>>)
    private fun parseClientAndServer(properties: Map<String, String>) = zipOrAccumulate(
        { parseClient(properties) },
        { parseServer(properties) },
    ) { client, server ->
        BuildConfiguration.ClientAndServer(
            client.configuration,
            client.platforms,
            server.configuration,
            server.platforms,
            parseBuildTargets(properties),
        )
    }
}
