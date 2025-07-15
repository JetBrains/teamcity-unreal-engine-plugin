package com.jetbrains.teamcity.plugins.unrealengine.server.discovery

import com.jetbrains.teamcity.plugins.unrealengine.common.UnrealEngineProject
import com.jetbrains.teamcity.plugins.unrealengine.common.UnrealPluginLoggers
import jetbrains.buildServer.util.browser.Element

class UprojectDirsFileDiscoverer(
    private val projectFileDiscoverer: UprojectFileDiscoverer,
) : UnrealProjectDiscoverer {
    companion object {
        private val logger = UnrealPluginLoggers.get<UprojectDirsFileDiscoverer>()
        private const val PROJECT_DIR_FILE_EXTENSION = ".uprojectdirs"
    }

    override fun discover(directory: Element): Collection<UnrealEngineProject> =
        directory.children?.let { children ->
            children
                .filter { it.isContentAvailable && it.fullName.endsWith(PROJECT_DIR_FILE_EXTENSION) }
                .flatMap {
                    getProjectPathsFrom(it)
                }.distinct()
                .flatMap {
                    val element = directory.browser.getElement(it)
                    if (element != null) {
                        projectFileDiscoverer.discover(element)
                    } else {
                        logger.debug("Project search path $it referenced by .uprojectdirs was not found, ignoring")
                        emptyList()
                    }
                }
        } ?: emptyList()

    private fun getProjectPathsFrom(projectDirsFile: Element): Collection<String> {
        val commentPrefix = ';'
        val reader = projectDirsFile.inputStream.bufferedReader()
        return try {
            reader
                .readLines()
                .map { it.trim() }
                .filter { !it.startsWith(commentPrefix) }
        } catch (e: Throwable) {
            logger.error("An error occurred during read of the file ${projectDirsFile.fullName}")
            emptyList()
        } finally {
            reader.close()
        }
    }
}
