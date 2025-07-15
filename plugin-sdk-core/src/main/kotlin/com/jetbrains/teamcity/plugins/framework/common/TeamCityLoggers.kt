package com.jetbrains.teamcity.plugins.framework.common

import com.intellij.openapi.diagnostic.Logger

class TeamCityLoggers {
    companion object {
        const val BASE_CATEGORY = "jetbrains.buildServer"

        // teamcity-agent.log / teamcity-server.log
        inline fun <reified T> get() = Logger.getInstance(BASE_CATEGORY + "." + T::class.java.name)

        // teamcity-build.log
        fun buildStdOut(): Logger = Logger.getInstance("teamcity.out")
    }
}
