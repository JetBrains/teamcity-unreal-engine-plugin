package com.jetbrains.teamcity.plugins.unrealengine.server.discovery

import com.jetbrains.teamcity.plugins.framework.common.TeamCityLoggers
import com.jetbrains.teamcity.plugins.unrealengine.common.JsonEncoder
import com.jetbrains.teamcity.plugins.unrealengine.common.UnrealEngineIdentifier
import com.jetbrains.teamcity.plugins.unrealengine.common.UnrealEngineProject
import com.jetbrains.teamcity.plugins.unrealengine.common.UnrealProjectPath
import com.jetbrains.teamcity.plugins.unrealengine.common.UnrealTargetPlatform
import jetbrains.buildServer.util.browser.Element
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.decodeFromStream

/**
 * In-memory representation of a .uproject file
 */
@Serializable
private data class UnrealEngineProjectDescriptor(
    @SerialName("EngineAssociation")
    val engineAssociation: String? = null,
    @SerialName("TargetPlatforms")
    val targetPlatforms: Collection<String>? = null,
)

class UprojectFileDiscoverer : UnrealProjectDiscoverer {
    companion object {
        private const val PROJECT_EXTENSION = ".uproject"
        private val logger = TeamCityLoggers.server<UprojectFileDiscoverer>()
    }

    override fun discover(directory: Element): Collection<UnrealEngineProject> =
        directory.children?.let { children ->
            children
                .filter { it.isContentAvailable && it.fullName.endsWith(PROJECT_EXTENSION) }
                .mapNotNull {
                    logger.debug("File ${it.fullName} seems to be an Unreal project file, trying to read it")
                    deserialize(it)
                }
        } ?: emptyList()

    @OptIn(ExperimentalSerializationApi::class)
    private fun deserialize(element: Element): UnrealEngineProject? {
        val stream = element.inputStream

        return try {
            val dto = JsonEncoder.instance.decodeFromStream<UnrealEngineProjectDescriptor>(stream)

            UnrealEngineProject(
                UnrealProjectPath(element.fullName),
                if (dto.engineAssociation == null) null else UnrealEngineIdentifier(dto.engineAssociation),
                dto.targetPlatforms
                    ?.map { UnrealTargetPlatform(it) }
                    ?: emptyList(),
            )
        } catch (e: Throwable) {
            logger.error("An error occurred during read of .ueproject file ${element.fullName}", e)
            null
        } finally {
            stream.close()
        }
    }
}
