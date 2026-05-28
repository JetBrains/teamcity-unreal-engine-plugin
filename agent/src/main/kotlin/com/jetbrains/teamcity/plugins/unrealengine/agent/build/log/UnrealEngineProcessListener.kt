package com.jetbrains.teamcity.plugins.unrealengine.agent.build.log

import com.jetbrains.teamcity.plugins.framework.common.TeamCityLoggers
import com.jetbrains.teamcity.plugins.unrealengine.agent.UnrealBuildContext
import com.jetbrains.teamcity.plugins.unrealengine.agent.buildcookrun.BuildCookRunWorkflowCreator
import com.jetbrains.teamcity.plugins.unrealengine.common.UnrealPluginLoggers
import jetbrains.buildServer.BuildProblemData
import jetbrains.buildServer.agent.BuildProgressLogger
import jetbrains.buildServer.agent.runner.ProcessListenerAdapter

class UnrealEngineProcessListenerFactory(
    private val logEventParser: UnrealLogEventParser,
) {
    context(context: UnrealBuildContext)
    fun create(
        vararg handlers: LogEventHandler,
        reportErrorsAsBuildProblems: Boolean = true,
    ) =
        UnrealEngineProcessListener(
            context.build.buildLogger,
            logEventParser,
            handlers.asList(),
            reportErrorsAsBuildProblems,
        )
}

class UnrealEngineProcessListener(
    private val buildLogger: BuildProgressLogger,
    private val logEventParser: UnrealLogEventParser,
    private val handlers: Collection<LogEventHandler>,
    private val reportErrorsAsBuildProblems: Boolean = true,
) : ProcessListenerAdapter() {
    companion object {
        private val agentLogger = UnrealPluginLoggers.get<BuildCookRunWorkflowCreator>()
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

                if (reportErrorsAsBuildProblems) {
                    buildLogger.logBuildProblem(event.asBuildProblem())
                } else {
                    buildLogger.warning(event.message)
                }
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
