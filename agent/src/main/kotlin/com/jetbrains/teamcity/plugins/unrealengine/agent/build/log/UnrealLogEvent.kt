package com.jetbrains.teamcity.plugins.unrealengine.agent.build.log

import java.time.Instant

data class UnrealLogEvent(
    val time: Instant,
    val level: LogLevel,
    val message: String,
    val channel: String? = null,
) {
    fun toLogString() =
        """Time: $time
             |Level: $level
             |Channel: ${channel ?: "N/A"}
             |Message: "$message"
        """.trimMargin()
}
