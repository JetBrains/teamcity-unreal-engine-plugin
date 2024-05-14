package com.jetbrains.teamcity.plugins.framework.agent

data class TeamCityParameter(
    val key: String,
    val value: String,
    val type: Type
) {
    enum class Type {
        ConfigurationParameter,
        EnvironmentVariable,
        SystemProperty,
    }
}

fun interface AgentParametersProvider {
    suspend fun provide(): List<TeamCityParameter>
}
