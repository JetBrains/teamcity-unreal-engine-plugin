package com.jetbrains.teamcity.plugins.unrealengine.agent

import arrow.core.getOrElse
import arrow.core.raise.either
import com.jetbrains.teamcity.plugins.unrealengine.agent.buildcookrun.BuildCookRunWorkflowCreator
import com.jetbrains.teamcity.plugins.unrealengine.agent.buildgraph.BuildGraphWorkflowCreator
import com.jetbrains.teamcity.plugins.unrealengine.agent.runautomation.RunAutomationWorkflowCreator
import com.jetbrains.teamcity.plugins.unrealengine.common.UnrealCommandType
import com.jetbrains.teamcity.plugins.unrealengine.common.UnrealEngineRunner
import com.jetbrains.teamcity.plugins.unrealengine.common.parameters.UnrealCommandTypeParameter
import jetbrains.buildServer.RunBuildException
import jetbrains.buildServer.agent.AgentBuildRunnerInfo
import jetbrains.buildServer.agent.BuildAgentConfiguration
import jetbrains.buildServer.agent.BuildRunnerContext
import jetbrains.buildServer.agent.runner.MultiCommandBuildSession
import jetbrains.buildServer.agent.runner.MultiCommandBuildSessionFactory
import kotlin.io.path.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.pathString

class UnrealEngineBuildSessionFactory(
    private val buildCookRunWorkflowCreator: BuildCookRunWorkflowCreator,
    private val buildGraphWorkflowCreator: BuildGraphWorkflowCreator,
    private val automationWorkflowCreator: RunAutomationWorkflowCreator,
) : MultiCommandBuildSessionFactory {

    override fun createSession(runnerContext: BuildRunnerContext): MultiCommandBuildSession = either {
        val commandType = UnrealCommandTypeParameter.parse(runnerContext.runnerParameters)

        val workflowCreator = when (commandType) {
            UnrealCommandType.BuildCookRun -> buildCookRunWorkflowCreator
            UnrealCommandType.BuildGraph -> buildGraphWorkflowCreator
            UnrealCommandType.RunAutomation -> automationWorkflowCreator
        }

        UnrealEngineBuildSession(workflowCreator, createUnrealBuildContext(runnerContext))
    }.getOrElse { throw RunBuildException("Unable to create build session. ${it.message}") }

    override fun getBuildRunnerInfo(): AgentBuildRunnerInfo = object : AgentBuildRunnerInfo {
        override fun getType() = UnrealEngineRunner.RUN_TYPE

        // since users might build the engine from source, there aren't any specific details we can rely on at this point
        // to determine whether the runner can launch. Therefore, the simplest approach is to always answer "yes"
        override fun canRun(agentConfiguration: BuildAgentConfiguration) = true
    }

    private fun createUnrealBuildContext(runnerContext: BuildRunnerContext): UnrealBuildContext {
        return object : UnrealBuildContext {
            override val workingDirectory = runnerContext.workingDirectory.canonicalPath
            override val agentTempDirectory = runnerContext.build.agentTempDirectory.canonicalPath

            override fun concatPaths(root: String, path: String): String = Path(root).resolve(path).pathString
            override fun fileExists(path: String) = Path(path).exists()
            override fun isAbsolute(path: String) = Path(path).isAbsolute
            override fun createDirectory(root: String, vararg parts: String) =
                Path(root, *parts).createDirectories().pathString

            override val buildParameters = runnerContext.buildParameters
            override val runnerParameters = runnerContext.runnerParameters
            override val build = runnerContext.build
            override val runnerId = runnerContext.id
            override val runnerName = runnerContext.name
        }
    }
}
