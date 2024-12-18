package com.jetbrains.teamcity.plugins.unrealengine.agent.build.log

interface LogEventHandler {
    fun tryHandleEvent(event: UnrealLogEvent): Boolean
}
