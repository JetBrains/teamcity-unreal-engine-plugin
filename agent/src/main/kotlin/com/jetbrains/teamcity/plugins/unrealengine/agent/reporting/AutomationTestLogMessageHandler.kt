package com.jetbrains.teamcity.plugins.unrealengine.agent.reporting

import com.jetbrains.teamcity.plugins.framework.common.TeamCityLoggers
import com.jetbrains.teamcity.plugins.unrealengine.agent.UnrealBuildContext
import jetbrains.buildServer.messages.serviceMessages.*

context(UnrealBuildContext)
class AutomationTestLogMessageHandler : LogMessageHandler {
    override fun tryHandleMessage(text: String): Boolean = tryHandleTestStarted(text) || tryHandleTestCompleted(text)

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

    private fun writeServiceMessages(messages: Sequence<ServiceMessage>) = messages.forEach { build.buildLogger.message(it.asString()) }

    private fun writeOriginalMessage(message: String) {
        build.buildLogger.message(message)
        buildStdOutLogger.info(message)
    }

    companion object {
        private val buildStdOutLogger = TeamCityLoggers.buildStdOut()
    }
}
