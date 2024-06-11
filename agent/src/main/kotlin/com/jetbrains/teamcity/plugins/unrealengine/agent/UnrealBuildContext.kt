package com.jetbrains.teamcity.plugins.unrealengine.agent

import com.jetbrains.teamcity.plugins.unrealengine.common.CommandExecutionContext
import jetbrains.buildServer.agent.AgentRunningBuild
import jetbrains.buildServer.agent.BuildParametersMap

interface UnrealBuildContext :
    TeamCityContext,
    CommandExecutionContext

interface TeamCityContext {
    val buildParameters: BuildParametersMap
    val runnerParameters: Map<String, String>
    val build: AgentRunningBuild
    val runnerId: String
    val runnerName: String
}
