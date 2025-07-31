package com.jetbrains.teamcity.plugins.framework.resource.location

import arrow.core.raise.Raise
import com.jetbrains.teamcity.plugins.framework.common.raise
import java.io.Reader

context(_: Raise<ResourceLocationResult.Error>)
@PublishedApi internal fun Reader.text(): String = try {
    readText()
} catch (e: Throwable) {
    raise(ResourceLocationResult.Error("Unknown error occurred during text read from the reader", e))
} finally {
    close()
}
