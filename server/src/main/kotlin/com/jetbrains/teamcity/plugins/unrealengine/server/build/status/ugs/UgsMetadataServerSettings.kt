package com.jetbrains.teamcity.plugins.unrealengine.server.build.status.ugs

import jetbrains.buildServer.web.functions.InternalProperties
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class UgsMetadataServerSettings {
    private val defaultTimeout = 5.seconds

    val requestTimeout: Duration
        get() =
            Duration.parseIsoStringOrNull(
                InternalProperties.getProperty(
                    "teamcity.internal.unreal-engine.ugs-metadata-server.request-timeout",
                    defaultTimeout.toIsoString(),
                ),
            ) ?: defaultTimeout

    val retryCount: Int
        get() = InternalProperties.getInteger("teamcity.internal.unreal-engine.ugs-metadata-server.retry-count", 3)
}
