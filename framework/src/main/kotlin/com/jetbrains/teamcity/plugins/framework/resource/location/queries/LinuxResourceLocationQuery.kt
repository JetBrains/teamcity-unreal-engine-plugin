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

interface LinuxResourceLocationContext : FileSystem

class LinuxResourceLocationQuery<T> @PublishedApi internal constructor (
    getValue: context(LinuxResourceLocationContext, Raise<ResourceLocationResult.Error>) () -> T
) : ResourceLocationQuery<LinuxResourceLocationContext, T>(getValue) {
    /**
     * Checks existence of a given file and read it.
     * @param fileName name of the file to read (absolute path).
     */
    fun file(fileName: String) = LinuxResourceLocationQuery {
        readFile(fileName)
    }
}

/**
 * Parses json content using current reader in [LinuxResourceLocationQuery].
 * @param T the type to deserialize the JSON value into.
 */
inline fun <reified T> LinuxResourceLocationQuery<Reader>.json(json: Json) = LinuxResourceLocationQuery {
    execute().parseJson<T>(json)
}

/**
 * Parses ini content using current reader in [LinuxResourceLocationQuery].
 * @param sectionName section whose properties you want to read. Read top-level properties if omitted.
 */
fun LinuxResourceLocationQuery<Reader>.ini(sectionName: String? = null) = LinuxResourceLocationQuery {
    execute().parseIni(sectionName)
}

fun <T, R> LinuxResourceLocationQuery<T>.map(transform: (T) -> R) = LinuxResourceLocationQuery {
    transform(execute())
}

/**
 * Reads lines using current reader in [LinuxResourceLocationQuery]
 * @param accept filter used to return only matching lines.
 * @param continueReading callback that is called after reading each line to decide if we should continue reading.
 */
fun LinuxResourceLocationQuery<Reader>.filteredLines(
    accept: AcceptFilter,
    continueReading: ReadContinuationDecision,
) = LinuxResourceLocationQuery {
    execute().filteredLines(accept, continueReading)
}
