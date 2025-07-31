package com.jetbrains.teamcity.plugins.framework.resource.location

import arrow.core.raise.Raise
import arrow.core.raise.catch
import com.jetbrains.teamcity.plugins.framework.common.raise
import kotlinx.serialization.json.Json
import java.io.Reader

context(_: Raise<ResourceLocationResult.Error>)
@PublishedApi internal inline fun <reified T> Reader.parseJson(json: Json): T = catch({
    json.decodeFromString<T>(text())
}) {
    raise(ResourceLocationResult.Error("An error occurred during parsing json", it))
}
