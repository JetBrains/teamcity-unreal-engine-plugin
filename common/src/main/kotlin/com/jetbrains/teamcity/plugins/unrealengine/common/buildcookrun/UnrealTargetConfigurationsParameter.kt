package com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun

import arrow.core.NonEmptyList
import arrow.core.raise.Raise
import arrow.core.raise.ensureNotNull
import com.jetbrains.teamcity.plugins.unrealengine.common.PropertyValidationError
import com.jetbrains.teamcity.plugins.unrealengine.common.UnrealTargetConfiguration
import com.jetbrains.teamcity.plugins.unrealengine.common.parameters.MultiSelectParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.parameters.SelectOption
import jetbrains.buildServer.parameters.ReferencesResolverUtil

object UnrealTargetConfigurationsParameter {
    private val options = UnrealTargetConfiguration.knownConfigurations.map { SelectOption(it.value) }
    private const val SEPARATOR = "+"

    fun joinConfigurations(configurations: Collection<UnrealTargetConfiguration>) =
        configurations.joinToString(separator = SEPARATOR) {
            it.value
        }

    object Standalone : MultiSelectParameter() {
        override val name = "target-configurations"
        override val displayName = "Target configurations"
        override val defaultValue = UnrealTargetConfiguration.Development.value
        override val required = true
        override val allowCustomValues = false
        override val description = null
        override val options = UnrealTargetConfigurationsParameter.options
        override val separator = SEPARATOR
    }

    object Client : MultiSelectParameter() {
        override val name = "client-target-configurations"
        override val displayName = "Client target configurations"
        override val defaultValue = UnrealTargetConfiguration.Development.value
        override val required = true
        override val allowCustomValues = false
        override val description = null
        override val options = UnrealTargetConfigurationsParameter.options
        override val separator = SEPARATOR
    }

    object Server : MultiSelectParameter() {
        override val name = "server-target-configurations"
        override val displayName = "Server target configurations"
        override val defaultValue = UnrealTargetConfiguration.Development.value
        override val required = true
        override val allowCustomValues = false
        override val description = null
        override val options = UnrealTargetConfigurationsParameter.options
        override val separator = SEPARATOR
    }

    context(Raise<PropertyValidationError>)
    fun parseTargetConfigurations(
        runnerParameters: Map<String, String>,
        name: String,
    ): NonEmptyList<UnrealTargetConfiguration> {
        val configurationsRaw = runnerParameters[name]
        ensureNotNull(configurationsRaw) { PropertyValidationError(name, "Target configuration list is missing") }

        val configurations =
            configurationsRaw
                .split(SEPARATOR)
                .map { UnrealTargetConfiguration(it) }
                .filter { configuration ->
                    UnrealTargetConfiguration.knownConfigurations.contains(configuration) ||
                        ReferencesResolverUtil.isReference(configuration.value)
                }
        if (configurations.isEmpty()) {
            raise(
                PropertyValidationError(
                    name,
                    "At least one target configuration must be specified. " +
                        "Valid values are: ${UnrealTargetConfiguration.knownConfigurations.joinToString()}",
                ),
            )
        } else {
            return NonEmptyList(configurations.first(), configurations.drop(1))
        }
    }
}
