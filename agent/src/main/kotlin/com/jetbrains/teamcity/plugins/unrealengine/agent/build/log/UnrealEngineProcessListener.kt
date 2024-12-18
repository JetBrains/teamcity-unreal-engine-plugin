package com.jetbrains.teamcity.plugins.unrealengine.agent.build.log

import com.jetbrains.teamcity.plugins.framework.common.TeamCityLoggers
import com.jetbrains.teamcity.plugins.unrealengine.agent.UnrealBuildContext
import com.jetbrains.teamcity.plugins.unrealengine.agent.buildcookrun.BuildCookRunWorkflowCreator
import jetbrains.buildServer.BuildProblemData
import jetbrains.buildServer.agent.BuildProgressLogger
import jetbrains.buildServer.agent.runner.ProcessListenerAdapter

class UnrealEngineProcessListenerFactory(
    private val logEventParser: UnrealLogEventParser,
) {
    context(UnrealBuildContext)
    fun create(vararg handlers: LogEventHandler) =
        UnrealEngineProcessListener(build.buildLogger, logEventParser, handlers.asList())
}

class UnrealEngineProcessListener(
    private val buildLogger: BuildProgressLogger,
    private val logEventParser: UnrealLogEventParser,
    private val handlers: Collection<LogEventHandler>,
) : ProcessListenerAdapter() {
    companion object {
        private val agentLogger = TeamCityLoggers.agent<BuildCookRunWorkflowCreator>()
        private val buildStdOutLogger = TeamCityLoggers.buildStdOut()
    }

    override fun onStandardOutput(text: String) {
        val event = logEventParser.parse(text)

        if (event.message.isEmpty()) {
            return
        }

        val handler = handlers.firstOrNull { it.tryHandleEvent(event) }
        if (handler != null) {
            agentLogger.debug(
                """Log event was handled by "${handler::class.simpleName}":
                |${event.toLogString()}
                """.trimMargin(),
            )
            return
        }

        when (event.level) {
            LogLevel.Error, LogLevel.Critical -> {
                buildStdOutLogger.error(event.message)
                buildLogger.logBuildProblem(event.asBuildProblem())
            }
            LogLevel.Warning -> {
                buildStdOutLogger.warn(event.message)
                buildLogger.warning(event.message)
            }
            else -> {
                buildStdOutLogger.info(event.message)
                buildLogger.message(event.message)
            }
        }
    }

    override fun onErrorOutput(text: String) {
        buildLogger.warning(text)
        buildStdOutLogger.warn(text)
    }

    private fun UnrealLogEvent.asBuildProblem(): BuildProblemData? {
        val problemType =
            channel?.let { "unreal-engine-error:$it" }
                ?: "unreal-engine-error"

        val problemId = "$problemType:$message".hashCode().toString()

        return BuildProblemData.createBuildProblem(
            problemId,
            problemType,
            message,
        )
    }
}
