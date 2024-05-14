package com.jetbrains.teamcity.plugins.unrealengine.agent

import com.jetbrains.teamcity.plugins.framework.common.TeamCityLoggers
import com.jetbrains.teamcity.plugins.unrealengine.agent.buildcookrun.BuildCookRunWorkflowCreator
import com.jetbrains.teamcity.plugins.unrealengine.agent.reporting.LogMessageHandler
import jetbrains.buildServer.agent.BuildProgressLogger
import jetbrains.buildServer.agent.runner.ProcessListenerAdapter

class UnrealEngineProcessListener private constructor(
    private val buildLogger: BuildProgressLogger,
    private val handlers: Collection<LogMessageHandler>,
) : ProcessListenerAdapter() {
    companion object {
        private val agentLogger = TeamCityLoggers.agent<BuildCookRunWorkflowCreator>()
        private val buildStdOutLogger = TeamCityLoggers.buildStdOut()

        context(UnrealBuildContext)
        fun create(vararg handlers: LogMessageHandler): UnrealEngineProcessListener {
            return UnrealEngineProcessListener(build.buildLogger, handlers.asList())
        }
    }

    override fun onStandardOutput(text: String) {
        val handler = handlers
            .firstOrNull { it.tryHandleMessage(text) }
            ?.also { agentLogger.debug("\"$text\" was handled by \"${it::class.simpleName}\"") }

        if (handler == null) {
            buildLogger.message(text)
            buildStdOutLogger.info(text)
        }
    }

    override fun onErrorOutput(text: String) {
        buildLogger.warning(text)
        buildStdOutLogger.warn(text)
    }
}
