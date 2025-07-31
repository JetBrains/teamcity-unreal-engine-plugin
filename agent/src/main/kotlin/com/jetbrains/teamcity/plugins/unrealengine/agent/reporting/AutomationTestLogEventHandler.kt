package com.jetbrains.teamcity.plugins.unrealengine.agent.reporting

import com.jetbrains.teamcity.plugins.framework.common.TeamCityLoggers
import com.jetbrains.teamcity.plugins.unrealengine.agent.UnrealBuildContext
import com.jetbrains.teamcity.plugins.unrealengine.agent.build.log.LogEventHandler
import com.jetbrains.teamcity.plugins.unrealengine.agent.build.log.UnrealLogEvent
import jetbrains.buildServer.messages.serviceMessages.ServiceMessage

class AutomationTestLogEventHandler(
    private val context: UnrealBuildContext,
) : LogEventHandler {
    override fun tryHandleEvent(event: UnrealLogEvent): Boolean =
        tryHandleTestStarted(event.message) || tryHandleTestCompleted(event.message)

    private fun tryHandleTestStarted(text: String): Boolean {
        val testStarted = AutomationTestLogParser.tryParseTestStarted(text) ?: return false
        writeServiceMessages(testStarted.asServiceMessages())
        writeOriginalMessage(text)
        return true
    }

    private fun tryHandleTestCompleted(text: String): Boolean {
        val testCompleted = AutomationTestLogParser.tryParseTestCompleted(text) ?: return false
        writeOriginalMessage(text)
        writeServiceMessages(testCompleted.asServiceMessages())
        return true
    }

    private fun writeServiceMessages(messages: Sequence<ServiceMessage>) =
        messages.forEach { context.build.buildLogger.message(it.asString()) }

    private fun writeOriginalMessage(message: String) {
        context.build.buildLogger.message(message)
        buildStdOutLogger.info(message)
    }

    companion object {
        private val buildStdOutLogger = TeamCityLoggers.buildStdOut()
    }
}
