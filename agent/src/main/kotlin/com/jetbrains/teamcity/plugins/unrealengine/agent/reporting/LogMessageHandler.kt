package com.jetbrains.teamcity.plugins.unrealengine.agent.reporting

interface LogMessageHandler {
    fun tryHandleMessage(text: String): Boolean
}
