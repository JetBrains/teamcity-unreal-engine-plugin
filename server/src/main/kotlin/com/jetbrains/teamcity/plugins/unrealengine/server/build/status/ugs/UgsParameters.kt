package com.jetbrains.teamcity.plugins.unrealengine.server.build.status.ugs

import arrow.core.Either
import arrow.core.NonEmptyList
import arrow.core.raise.Raise
import arrow.core.raise.either
import arrow.core.raise.zipOrAccumulate
import com.jetbrains.teamcity.plugins.framework.common.ensure
import com.jetbrains.teamcity.plugins.unrealengine.common.PropertyValidationError
import com.jetbrains.teamcity.plugins.unrealengine.common.parameters.TextInputParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.ugs.UgsMetadataServerUrl

object ServerUrlParameter : TextInputParameter {
    override val description = null
    override val supportsVcsNavigation = false
    override val expandable = false
    override val required = true
    override val advanced = false
    override val name = "ugs-metadata-server-server-url"
    override val displayName = "The metadata server URL"
    override val defaultValue = ""

    context(_: Raise<PropertyValidationError>)
    fun parse(properties: Map<String, String>): UgsMetadataServerUrl {
        val serverUrl = properties[name]?.trim()
        ensure(!serverUrl.isNullOrEmpty()) {
            PropertyValidationError(
                name,
                "The metadata server URL cannot be empty",
            )
        }

        return UgsMetadataServerUrl(serverUrl)
    }
}

object BadgeNameParameter : TextInputParameter {
    override val description = "The public badge name that will appear in UGS"
    override val supportsVcsNavigation = false
    override val expandable = false
    override val required = true
    override val advanced = false
    override val name = "ugs-metadata-server-badge-name"
    override val displayName = "Badge"
    override val defaultValue = ""

    context(_: Raise<PropertyValidationError>)
    fun parse(properties: Map<String, String>): UgsBadgeName {
        val badgeName = properties[name]?.trim()
        ensure(!badgeName.isNullOrEmpty()) { PropertyValidationError(name, "The badge name cannot be empty") }

        return UgsBadgeName(badgeName)
    }
}

object ProjectParameter : TextInputParameter {
    override val description =
        """
        The depot path to the project that should be decorated with this badge. This path must point to a directory, not an .uproject file.
        Example: //UE5/Main/Samples/Games/Lyra
        """.trimIndent()
    override val supportsVcsNavigation = false
    override val expandable = false
    override val required = true
    override val advanced = false
    override val name = "ugs-metadata-server-project-path"
    override val displayName = "Project"
    override val defaultValue = ""

    context(_: Raise<PropertyValidationError>)
    fun parse(properties: Map<String, String>): UgsProject {
        val project = properties[name]?.trim()
        ensure(!project.isNullOrEmpty()) { PropertyValidationError(name, "The project path cannot be empty") }

        return UgsProject(project)
    }
}

@JvmInline
value class UgsBadgeName(
    val value: String,
)

@JvmInline
value class UgsProject(
    val value: String,
)

data class UgsBuildFeatureParameters(
    val serverUrl: UgsMetadataServerUrl,
    val badgeName: UgsBadgeName,
    val project: UgsProject,
)

class UgsParametersParser {
    fun parse(properties: Map<String, String>): Either<NonEmptyList<PropertyValidationError>, UgsBuildFeatureParameters> =
        either {
            zipOrAccumulate(
                { ServerUrlParameter.parse(properties) },
                { BadgeNameParameter.parse(properties) },
                { ProjectParameter.parse(properties) },
            ) { serverUrl, badgeName, project -> UgsBuildFeatureParameters(serverUrl, badgeName, project) }
        }
}
