package com.jetbrains.teamcity.plugins.unrealengine.agent

import com.jetbrains.teamcity.plugins.framework.agent.AgentParametersProvider
import com.jetbrains.teamcity.plugins.framework.agent.TeamCityParameter
import com.jetbrains.teamcity.plugins.framework.common.Environment
import com.jetbrains.teamcity.plugins.framework.common.TeamCityLoggers
import com.jetbrains.teamcity.plugins.framework.resource.location.IniProperty
import com.jetbrains.teamcity.plugins.framework.resource.location.ResourceLocationResult
import com.jetbrains.teamcity.plugins.framework.resource.location.ResourceLocator
import com.jetbrains.teamcity.plugins.framework.resource.location.queries.ini
import com.jetbrains.teamcity.plugins.framework.resource.location.queries.json
import com.jetbrains.teamcity.plugins.framework.resource.location.queries.map
import com.jetbrains.teamcity.plugins.framework.resource.location.windows.registry.WindowsRegistryEntry
import com.jetbrains.teamcity.plugins.framework.resource.location.windows.registry.WindowsRegistrySearchFilter
import com.jetbrains.teamcity.plugins.unrealengine.common.JsonEncoder
import com.jetbrains.teamcity.plugins.unrealengine.common.UnrealEngineRunner
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

private data class UnrealEngineInstallation(
    val location: String,
    val identifier: String,
)

@Serializable
private class InstallationInfoList(
    @SerialName("InstallationList")
    val installationList: List<InstallationInfo>,
) {
    @Serializable
    class InstallationInfo(
        @SerialName("InstallLocation")
        val installLocation: String,
        @SerialName("AppVersion")
        val appVersion: String,
    )

    fun toUnrealEngineInfo(): List<UnrealEngineInstallation> =
        installationList.map {
            // version usually looks like this: "5.1.0-23058290+++UE5+Release-5.1-2022.1.1-23428875-Mac"
            UnrealEngineInstallation(it.installLocation, it.appVersion.takeWhile { char -> char != '-' })
        }
}

class EngineInstallationParametersProvider(
    private val resourceLocator: ResourceLocator,
    private val environment: Environment,
) : AgentParametersProvider {
    companion object {
        private val logger = TeamCityLoggers.agent<EngineInstallationParametersProvider>()
        private val json = JsonEncoder.instance
    }

    override suspend fun provide(): List<TeamCityParameter> = locateInstallations()

    private suspend fun locateInstallations(): List<TeamCityParameter> {
        val results =
            resourceLocator.locateResources {
                macos(
                    {
                        file("${environment.homeDirectory}/Library/Application Support/Epic/UnrealEngineLauncher/LauncherInstalled.dat")
                            .json<InstallationInfoList>(json)
                            .map { it.toUnrealEngineInfo() }
                    },
                    {
                        file("${environment.homeDirectory}/Library/Application Support/Epic/UnrealEngine/Install.ini")
                            .ini("Installations")
                            .map { it.map { entry -> entry.toUnrealEngineInfo() } }
                    },
                )
                linux(
                    {
                        file("${environment.homeDirectory}/.config/Epic/UnrealEngineLauncher/LauncherInstalled.dat")
                            .json<InstallationInfoList>(json)
                            .map { it.toUnrealEngineInfo() }
                    },
                    {
                        file("${environment.homeDirectory}/.config/Epic/UnrealEngine/Install.ini")
                            .ini("Installations")
                            .map { it.map { entry -> entry.toUnrealEngineInfo() } }
                    },
                )
                windows(
                    {
                        file("${environment.programDataDirectory}/Epic/UnrealEngineLauncher/LauncherInstalled.dat")
                            .json<InstallationInfoList>(json)
                            .map { it.toUnrealEngineInfo() }
                    },
                    {
                        registry(
                            "HKEY_CURRENT_USER\\SOFTWARE\\Epic Games\\Unreal Engine\\Builds",
                            object : WindowsRegistrySearchFilter {
                                override fun accept(key: WindowsRegistryEntry.Key) = false

                                override fun accept(value: WindowsRegistryEntry.Value) = true
                            },
                        ).map {
                            it
                                .filterIsInstance<WindowsRegistryEntry.Value>()
                                .map { entry -> entry.toUnrealEngineInfo() }
                        }
                    },
                )
            }

        return results
            .flatMap { result ->
                when (result) {
                    is ResourceLocationResult.Error -> {
                        when (result.exception) {
                            null -> logger.info("One of the location queries ended up with an error: ${result.message}")
                            else ->
                                logger.info(
                                    "One of the location queries ended up with an error: ${result.message}",
                                    result.exception,
                                )
                        }
                        emptyList()
                    }
                    is ResourceLocationResult.Success -> result.data
                }
            }.map {
                logger.info("Discovered Unreal Engine installation. Identifier: ${it.identifier}, path: ${it.location}")
                TeamCityParameter(
                    "${UnrealEngineRunner.AGENT_PARAMETER_NAME_PREFIX}.${it.identifier}.path",
                    it.location,
                    TeamCityParameter.Type.ConfigurationParameter,
                )
            }
    }

    private fun WindowsRegistryEntry.Value.toUnrealEngineInfo(): UnrealEngineInstallation = UnrealEngineInstallation(data, name)

    private fun IniProperty.toUnrealEngineInfo(): UnrealEngineInstallation = UnrealEngineInstallation(value, key)
}
