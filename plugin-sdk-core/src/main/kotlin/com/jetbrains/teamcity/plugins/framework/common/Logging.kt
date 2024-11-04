package com.jetbrains.teamcity.plugins.framework.common

import com.intellij.openapi.diagnostic.Logger
import jetbrains.buildServer.log.Loggers

class TeamCityLoggers {
    companion object {
        // teamcity-agent.log / teamcity-server.log
        inline fun <reified T> get() = Logger.getInstance("jetbrains.buildServer." + T::class.java.name)
        // teamcity-agent.log
        inline fun <reified T> agent(): Logger = Logger.getInstance(Loggers.AGENT_CATEGORY + '.' + T::class.java.name)
        // teamcity-server.log
        inline fun <reified T> server(): Logger = Logger.getInstance(Loggers.SERVER_CATEGORY + '.' + T::class.java.name)
        // teamcity-build.log
        fun buildStdOut(): Logger = Logger.getInstance("teamcity.out")
    }
}
