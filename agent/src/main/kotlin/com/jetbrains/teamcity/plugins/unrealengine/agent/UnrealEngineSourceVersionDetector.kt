package com.jetbrains.teamcity.plugins.unrealengine.agent

import arrow.core.raise.Raise
import arrow.core.raise.recover
import com.jetbrains.teamcity.plugins.framework.common.TeamCityLoggers
import com.jetbrains.teamcity.plugins.framework.resource.location.ResourceLocationResult
import com.jetbrains.teamcity.plugins.framework.resource.location.ResourceLocator
import com.jetbrains.teamcity.plugins.framework.resource.location.queries.filteredLines
import com.jetbrains.teamcity.plugins.framework.resource.location.queries.json
import com.jetbrains.teamcity.plugins.framework.resource.location.queries.map
import com.jetbrains.teamcity.plugins.unrealengine.common.CommandExecutionContext
import com.jetbrains.teamcity.plugins.unrealengine.common.GenericError
import com.jetbrains.teamcity.plugins.unrealengine.common.JsonEncoder
import com.jetbrains.teamcity.plugins.unrealengine.common.UnrealEngineRootPath
import com.jetbrains.teamcity.plugins.unrealengine.common.UnrealEngineVersion
import com.jetbrains.teamcity.plugins.unrealengine.common.ensure
import com.jetbrains.teamcity.plugins.unrealengine.common.ensureNotNull
import com.jetbrains.teamcity.plugins.unrealengine.common.raise
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
private data class BuildVersion(
    @SerialName("MajorVersion")
    val majorVersion: Int,
    @SerialName("MinorVersion")
    val minorVersion: Int,
    @SerialName("PatchVersion")
    val patchVersion: Int,
) {
    companion object {
        const val PART_COUNT = 3
    }
}

class UnrealEngineSourceVersionDetector(
    private val resourceLocator: ResourceLocator,
) {
    companion object {
        private val logger = TeamCityLoggers.agent<UnrealEngineSourceVersionDetector>()
        private val json = JsonEncoder.instance
        private val versionPartRegex = "^#define\\s+ENGINE_(\\w+)_VERSION\\s+(\\d+)\$".toRegex()
    }

    context(Raise<GenericError>, CommandExecutionContext)
    suspend fun detect(engineRootPath: UnrealEngineRootPath): UnrealEngineVersion =
        recover({
            lookUpInBuildVersionFile(engineRootPath)
        }) {
            logger.warn(
                "An error occurred while searching in the 'Build.version' file: " +
                    "${it.message}. Proceeding to search in the 'Version.h' file",
            )
            lookUpInCppVersionHeaderFile(engineRootPath)
        }

    context(Raise<GenericError>, CommandExecutionContext)
    private suspend fun lookUpInBuildVersionFile(engineRootPath: UnrealEngineRootPath): UnrealEngineVersion {
        val locationResults =
            resourceLocator.locateResources {
                anyOS(
                    {
                        file(resolvePath(engineRootPath.value, "Engine/Build/Build.version"))
                            .json<BuildVersion>(json)
                            .map { UnrealEngineVersion(it.majorVersion, it.minorVersion, it.patchVersion) }
                    },
                )
            }

        val result =
            ensureNotNull(
                locationResults.firstOrNull(),
                "No results were found while attempting to search in the 'Build.version' file",
            )

        return when (result) {
            is ResourceLocationResult.Error -> raise(result.message)
            is ResourceLocationResult.Success -> {
                logger.info("Version '${result.data}' was found in 'Build.version' file")
                result.data
            }
        }
    }

    context(Raise<GenericError>, CommandExecutionContext)
    private suspend fun lookUpInCppVersionHeaderFile(engineRootPath: UnrealEngineRootPath): UnrealEngineVersion {
        val locationResults =
            resourceLocator.locateResources {
                anyOS(
                    {
                        file(resolvePath(engineRootPath.value, "Engine/Source/Runtime/Launch/Resources/Version.h"))
                            .filteredLines(
                                accept = { line -> versionPartRegex.matches(line) },
                                continueReading = { _, acceptedSoFar -> acceptedSoFar != BuildVersion.PART_COUNT },
                            )
                    },
                )
            }

        val result =
            ensureNotNull(
                locationResults.firstOrNull(),
                "No results were found while attempting to search in the 'Version.h' file",
            )

        return when (result) {
            is ResourceLocationResult.Error -> raise(result.message)
            is ResourceLocationResult.Success -> {
                val version = result.data.buildVersion()

                ensure(version != UnrealEngineVersion.empty, "Unable to find version information in 'Version.h' file")

                logger.info("Version '$version' was found in 'Version.h' file")

                version
            }
        }
    }

    private fun List<String>.buildVersion(): UnrealEngineVersion =
        fold(UnrealEngineVersion.empty) { currentVersion, foundLine ->
            versionPartRegex.matchEntire(foundLine)?.let {
                val (partName, partVersion) = it.destructured
                return@fold when (partName) {
                    "MAJOR" -> {
                        currentVersion.copy(major = partVersion.toInt())
                    }
                    "MINOR" -> {
                        currentVersion.copy(minor = partVersion.toInt())
                    }
                    "PATCH" -> {
                        currentVersion.copy(patch = partVersion.toInt())
                    }
                    else -> {
                        logger.warn(
                            "Found '$foundLine' line matched version regex '$versionPartRegex', " +
                                "but it doesn't contain expected version part (MAJOR, MINOR, PATCH), skipping",
                        )
                        currentVersion
                    }
                }
            }

            logger.warn(
                "Found line '$foundLine' doesn't match version line regex '$versionPartRegex', " +
                    "skipping",
            )

            currentVersion
        }
}
