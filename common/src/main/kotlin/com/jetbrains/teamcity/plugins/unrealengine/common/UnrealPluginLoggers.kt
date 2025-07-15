package com.jetbrains.teamcity.plugins.unrealengine.common

import com.intellij.openapi.diagnostic.Logger
import com.jetbrains.teamcity.plugins.framework.common.TeamCityLoggers

class UnrealPluginLoggers {
    companion object {
        // teamcity-agent.log / teamcity-server.log
        inline fun <reified T> get() = Logger.getInstance("${TeamCityLoggers.BASE_CATEGORY}.unrealEngine." + T::class.java.name)
    }
}
