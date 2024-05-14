package com.jetbrains.teamcity.plugins.framework.agent

import com.intellij.openapi.diagnostic.Logger
import com.jetbrains.teamcity.plugins.framework.common.TeamCityLoggers
import jetbrains.buildServer.agent.config.AgentParametersSupplier
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking

class PrimaryAgentParametersSupplier(
    providers: List<AgentParametersProvider>,
) : AgentParametersSupplier {

    companion object {
        private val logger: Logger = TeamCityLoggers.agent<PrimaryAgentParametersSupplier>()
    }

    private val parameters: List<TeamCityParameter> by lazy {
        logger.info("Retrieving agent parameters")

        val result = runBlocking {
            providers.map {
                async {
                    try {
                        it.provide()
                    } catch (e: Throwable) {
                        logger.error("An error occurred during parameters retrieval", e)
                        emptyList()
                    }
                }
            }
                .awaitAll()
                .flatten()
        }

        logger.info("Agent parameters retrieval finished. Number of retrieved parameters: ${result.size}")

        result
    }

    override fun getParameters(): MutableMap<String, String> = parameters
        .filter {it.type == TeamCityParameter.Type.ConfigurationParameter }
        .associate { it.key to it.value }
        .toMutableMap()

    override fun getEnvironmentVariables(): MutableMap<String, String> = parameters
        .filter {it.type == TeamCityParameter.Type.EnvironmentVariable }
        .associate { it.key to it.value }
        .toMutableMap()

    override fun getSystemProperties(): MutableMap<String, String> = parameters
        .filter {it.type == TeamCityParameter.Type.SystemProperty }
        .associate { it.key to it.value }
        .toMutableMap()
}
