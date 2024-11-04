package com.jetbrains.teamcity.plugins.framework.resource.location

import arrow.core.raise.Raise
import arrow.core.raise.ensure
import org.apache.commons.configuration2.INIConfiguration
import java.io.Reader

data class IniProperty(
    val key: String,
    val value: String,
)

context(Raise<ResourceLocationResult.Error>)
internal fun Reader.parseIni(sectionName: String? = null): List<IniProperty> {
    val config = INIConfiguration()

    try {
        config.read(this)
    } catch (e: Throwable) {
        raise(ResourceLocationResult.Error("Unknown error occurred during ini config read", e))
    } finally {
        close()
    }

    ensure(config.sections.contains(sectionName)) { raise(ResourceLocationResult.Error("Specified section $sectionName does not exist")) }

    val targetSection = config.getSection(sectionName)
    return targetSection
        .keys
        .asSequence()
        .map {
            IniProperty(parseIniKey(it), targetSection.getProperty(it).toString())
        }
        .toList()
}

private fun parseIniKey(key: String): String = key.replace("..", ".").removePrefix("{").removeSuffix("}")
