package com.jetbrains.teamcity.plugins.unrealengine.common

import kotlinx.serialization.json.Json

object JsonEncoder {
    val instance =
        Json {
            ignoreUnknownKeys = true
        }
}
