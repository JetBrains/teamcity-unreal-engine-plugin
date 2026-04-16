package com.jetbrains.teamcity.plugins.unrealengine.agent.reporting

import com.jetbrains.teamcity.plugins.unrealengine.agent.UnrealBuildContext
import com.jetbrains.teamcity.plugins.unrealengine.agent.build.log.LogEventHandler
import com.jetbrains.teamcity.plugins.unrealengine.agent.build.log.LogLevel
import com.jetbrains.teamcity.plugins.unrealengine.agent.build.log.UnrealLogEvent

class CommandletLogEventHandler(
    private val context: UnrealBuildContext,
) : LogEventHandler {
    private var warningCount = 0
    private var errorCount = 0

    override fun tryHandleEvent(event: UnrealLogEvent): Boolean {
        when (event.level) {
            LogLevel.Warning -> {
                warningCount++
                publishStatistic(WARNING_COUNT_KEY, warningCount)
            }

            LogLevel.Error, LogLevel.Critical -> {
                errorCount++
                publishStatistic(ERROR_COUNT_KEY, errorCount)
            }

            else -> Unit
        }

        // Let the normal listener pipeline continue to keep existing logging behavior.
        return false
    }

    private fun publishStatistic(
        key: String,
        value: Int,
    ) {
        context.build.buildLogger.message(
            "##teamcity[buildStatisticValue key='$key' value='$value']",
        )
    }

    companion object {
        const val WARNING_COUNT_KEY = "unreal.commandlet.warnings"
        const val ERROR_COUNT_KEY = "unreal.commandlet.errors"
    }
}

