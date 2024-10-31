package com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun

import arrow.core.NonEmptyList
import arrow.core.raise.Raise
import arrow.core.raise.ensureNotNull
import com.jetbrains.teamcity.plugins.unrealengine.common.PropertyValidationError
import com.jetbrains.teamcity.plugins.unrealengine.common.UnrealTargetPlatform
import com.jetbrains.teamcity.plugins.unrealengine.common.parameters.MultiSelectParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.parameters.SelectOption

object UnrealTargetPlatformsParameter {
    val options = UnrealTargetPlatform.knownPlatforms.map { SelectOption(it.value) }

    private const val SEPARATOR = "+"

    fun joinPlatforms(platforms: Collection<UnrealTargetPlatform>) =
        platforms
            .joinToString(separator = SEPARATOR) { it.value }

    object Standalone : MultiSelectParameter() {
        override val name = "target-platforms"
        override val displayName = "Target platforms"
        override val defaultValue = ""
        override val description = null
        override val allowCustomValues = true
        override val required = true
        override val separator = SEPARATOR

        override val options = UnrealTargetPlatformsParameter.options
    }

    object Client : MultiSelectParameter() {
        override val name = "client-target-platforms"
        override val displayName = "Client target platforms"
        override val defaultValue = ""
        override val description = null
        override val allowCustomValues = true
        override val required = true
        override val separator = SEPARATOR

        override val options = UnrealTargetPlatformsParameter.options
    }

    object Server : MultiSelectParameter() {
        override val name = "server-target-platforms"
        override val displayName = "Server target platforms"
        override val defaultValue = ""
        override val description = null
        override val allowCustomValues = true
        override val required = true
        override val separator = SEPARATOR

        override val options = UnrealTargetPlatformsParameter.options
    }

    context(Raise<PropertyValidationError>)
    fun parseTargetPlatforms(
        properties: Map<String, String>,
        name: String,
    ): NonEmptyList<UnrealTargetPlatform> {
        val platformsRaw = properties[name]
        ensureNotNull(platformsRaw) { PropertyValidationError(name, "Target platform list is missing") }

        val platforms =
            platformsRaw
                .split(SEPARATOR)
                .filter { it.isNotEmpty() }
                .map { UnrealTargetPlatform(it) }

        if (platforms.isEmpty()) {
            raise(PropertyValidationError(name, "At least one target platform must be specified"))
        } else {
            return NonEmptyList(platforms.first(), platforms.drop(1))
        }
    }
}
