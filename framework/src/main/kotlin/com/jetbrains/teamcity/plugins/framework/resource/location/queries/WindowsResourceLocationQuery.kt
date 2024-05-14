package com.jetbrains.teamcity.plugins.framework.resource.location.queries

import arrow.core.raise.Raise
import com.jetbrains.teamcity.plugins.framework.common.CommandLineRunner
import com.jetbrains.teamcity.plugins.framework.resource.location.AcceptFilter
import com.jetbrains.teamcity.plugins.framework.resource.location.FileSystem
import com.jetbrains.teamcity.plugins.framework.resource.location.ReadContinuationDecision
import com.jetbrains.teamcity.plugins.framework.resource.location.ResourceLocationResult
import com.jetbrains.teamcity.plugins.framework.resource.location.filteredLines
import com.jetbrains.teamcity.plugins.framework.resource.location.parseJson
import com.jetbrains.teamcity.plugins.framework.resource.location.readFile
import com.jetbrains.teamcity.plugins.framework.resource.location.windows.registry.WindowsRegistrySearchFilter
import com.jetbrains.teamcity.plugins.framework.resource.location.windows.registry.windowsRegistry
import kotlinx.serialization.json.Json
import java.io.Reader

interface WindowsResourceLocationContext : FileSystem {
    val commandLineRunner: CommandLineRunner
}

class WindowsResourceLocationQuery<T> @PublishedApi internal constructor (
    getValue: context(WindowsResourceLocationContext, Raise<ResourceLocationResult.Error>) () -> T
) : ResourceLocationQuery<WindowsResourceLocationContext, T>(getValue) {
    /**
     * Checks existence of a given file and read it.
     * @param fileName name of the file to read (absolute path).
     */
    fun file(fileName: String) = WindowsResourceLocationQuery {
        readFile(fileName)
    }

    /**
     * Tries to read a node on the given path and returns its entries matching the specified filter.
     * @param path to a node.
     * @param filter used to select resulting entries [WindowsRegistrySearchFilter].
     */
    fun registry(path: String, filter: WindowsRegistrySearchFilter) = WindowsResourceLocationQuery {
        windowsRegistry(path, filter)
    }
}

/**
 * Parses json content using current reader in [WindowsResourceLocationQuery].
 * @param T the type to deserialize the JSON value into.
 */
inline fun <reified T> WindowsResourceLocationQuery<Reader>.json(json: Json) = WindowsResourceLocationQuery {
    execute().parseJson<T>(json)
}

fun <T, R> WindowsResourceLocationQuery<T>.map(transform: (T) -> R) = WindowsResourceLocationQuery {
    transform(execute())
}

/**
 * Reads lines using current reader in [WindowsResourceLocationQuery]
 * @param accept filter used to return only matching lines.
 * @param continueReading callback that is called after reading each line to decide if we should continue reading.
 */
fun WindowsResourceLocationQuery<Reader>.filteredLines(
    accept: AcceptFilter,
    continueReading: ReadContinuationDecision,
) = WindowsResourceLocationQuery {
    execute().filteredLines(accept, continueReading)
}
