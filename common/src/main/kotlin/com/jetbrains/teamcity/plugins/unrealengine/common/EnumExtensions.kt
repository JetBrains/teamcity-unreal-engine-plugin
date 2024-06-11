package com.jetbrains.teamcity.plugins.unrealengine.common

inline fun <reified T : Enum<T>> enumValueOfOrNull(name: String): T? = runCatching { enumValueOf<T>(name) }.getOrNull()
