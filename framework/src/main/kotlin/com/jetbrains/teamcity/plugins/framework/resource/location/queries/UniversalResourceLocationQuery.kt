package com.jetbrains.teamcity.plugins.framework.resource.location.queries


import arrow.core.raise.Raise
import com.jetbrains.teamcity.plugins.framework.resource.location.AcceptFilter
import com.jetbrains.teamcity.plugins.framework.resource.location.FileSystem
import com.jetbrains.teamcity.plugins.framework.resource.location.ReadContinuationDecision
import com.jetbrains.teamcity.plugins.framework.resource.location.ResourceLocationResult
import com.jetbrains.teamcity.plugins.framework.resource.location.filteredLines
import com.jetbrains.teamcity.plugins.framework.resource.location.parseJson
import com.jetbrains.teamcity.plugins.framework.resource.location.readFile
import kotlinx.serialization.json.Json
import java.io.Reader

interface UniversalResourceLocationContext : FileSystem, ResourceLocationContext

class UniversalResourceLocationQuery<T> @PublishedApi internal constructor (
    getValue: context(UniversalResourceLocationContext, Raise<ResourceLocationResult.Error>) () -> T
) : ResourceLocationQuery<UniversalResourceLocationContext, T>(getValue) {
    /**
     * Checks existence of a given file and read it.
     * @param fileName name of the file to read (absolute path).
     */
    fun file(fileName: String) = UniversalResourceLocationQuery {
        readFile(fileName)
    }
}

/**
 * Parses json content using current reader in [UniversalResourceLocationQuery].
 * @param T the type to deserialize the JSON value into.
 */
inline fun <reified T> UniversalResourceLocationQuery<Reader>.json(json: Json)
    = UniversalResourceLocationQuery { execute().parseJson<T>(json) }

fun <T, R> UniversalResourceLocationQuery<T>.map(transform: (T) -> R) = UniversalResourceLocationQuery { transform(execute()) }

/**
 * Reads lines using current reader in [UniversalResourceLocationQuery]
 * @param accept filter used to return only matching lines.
 * @param continueReading callback that is called after reading each line to decide if we should continue reading.
 */
fun UniversalResourceLocationQuery<Reader>.filteredLines(
    accept: AcceptFilter,
    continueReading: ReadContinuationDecision,
) = UniversalResourceLocationQuery { execute().filteredLines(accept, continueReading) }
