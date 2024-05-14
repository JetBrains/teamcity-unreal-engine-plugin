package com.jetbrains.teamcity.plugins.framework.resource.location.windows.registry

import arrow.core.raise.Raise
import arrow.core.raise.catch
import arrow.core.raise.ensure
import com.jetbrains.teamcity.plugins.framework.common.CommandLineRunner
import com.jetbrains.teamcity.plugins.framework.resource.location.ResourceLocationResult
import com.jetbrains.teamcity.plugins.framework.resource.location.queries.WindowsResourceLocationContext

interface WindowsRegistrySearchFilter {
    fun accept(key: WindowsRegistryEntry.Key): Boolean
    fun accept(value: WindowsRegistryEntry.Value): Boolean
}

context(Raise<ResourceLocationResult.Error>, WindowsResourceLocationContext)
internal fun windowsRegistry(path: String, filter: WindowsRegistrySearchFilter): List<WindowsRegistryEntry> {
    val command = WindowsRegistryCommands.query(path)
    var registrySearchResult: CommandLineRunner.RunResult? = null
    catch({
        registrySearchResult = commandLineRunner.run(command)
    }) {
        raise(ResourceLocationResult.Error("Unknown error during Windows registry lookup", it))
    }

    ensure(registrySearchResult != null) {
        ResourceLocationResult.Error("Unknown result of a Windows registry lookup command. Perhaps it timed out")
    }

    return registrySearchResult!!.run {
        ensure(exitCode == 0) {
            raise(ResourceLocationResult.Error("Windows registry lookup ended with non-zero exit code"))
        }

        standardOutput
            .parseEntries(WindowsRegistryParser())
            .filter { it.match(filter) }
            .toList()
    }
}

private fun Collection<String>.parseEntries(parser: WindowsRegistryParser) = sequence {
    this@parseEntries.forEach {
        val key = parser.tryParseKey(it)
        if (key != null) {
            yield(key)
        }

        val value = parser.tryParseValue(it)
        if (value != null ) {
            yield(value)
        }
    }
}
