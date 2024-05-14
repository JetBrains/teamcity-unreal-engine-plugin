package com.jetbrains.teamcity.plugins.framework.resource.location.queries

import arrow.core.raise.Raise
import com.jetbrains.teamcity.plugins.framework.resource.location.AcceptFilter
import com.jetbrains.teamcity.plugins.framework.resource.location.FileSystem
import com.jetbrains.teamcity.plugins.framework.resource.location.ReadContinuationDecision
import com.jetbrains.teamcity.plugins.framework.resource.location.ResourceLocationResult
import com.jetbrains.teamcity.plugins.framework.resource.location.filteredLines
import com.jetbrains.teamcity.plugins.framework.resource.location.parseIni
import com.jetbrains.teamcity.plugins.framework.resource.location.parseJson
import com.jetbrains.teamcity.plugins.framework.resource.location.readFile
import kotlinx.serialization.json.Json
import java.io.Reader

interface MacOsResourceLocationContext : FileSystem

class MacOsResourceLocationQuery<T> @PublishedApi internal constructor (
    getValue: context(MacOsResourceLocationContext, Raise<ResourceLocationResult.Error>) () -> T
) : ResourceLocationQuery<MacOsResourceLocationContext, T>(getValue) {
    /**
     * Checks existence of a given file and read it.
     * @param fileName name of the file to read (absolute path).
     */
    fun file(fileName: String) = MacOsResourceLocationQuery {
        readFile(fileName)
    }
}

/**
 * Parses json content using current reader in [MacOsResourceLocationQuery].
 * @param T the type to deserialize the JSON value into.
 */
inline fun <reified T> MacOsResourceLocationQuery<Reader>.json(json: Json) = MacOsResourceLocationQuery {
    execute().parseJson<T>(json)
}

/**
 * Parses ini content using current reader in [MacOsResourceLocationQuery].
 * @param sectionName section whose properties you want to read. Read top-level properties if omitted.
 */
fun MacOsResourceLocationQuery<Reader>.ini(sectionName: String? = null) = MacOsResourceLocationQuery {
    execute().parseIni(sectionName)
}

fun <T, R> MacOsResourceLocationQuery<T>.map(transform: (T) -> R) = MacOsResourceLocationQuery {
    transform(execute())
}

/**
 * Reads lines using current reader in [MacOsResourceLocationQuery]
 * @param accept filter used to return only matching lines.
 * @param continueReading callback that is called after reading each line to decide if we should continue reading.
 */
fun MacOsResourceLocationQuery<Reader>.filteredLines(
    accept: AcceptFilter,
    continueReading: ReadContinuationDecision,
) = MacOsResourceLocationQuery {
    execute().filteredLines(accept, continueReading)
}
