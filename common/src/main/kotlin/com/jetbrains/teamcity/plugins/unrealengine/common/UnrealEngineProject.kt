package com.jetbrains.teamcity.plugins.unrealengine.common

@JvmInline
value class UnrealEngineIdentifier(
    val value: String,
)

data class UnrealEngineVersion(
    val major: Int,
    val minor: Int,
    val patch: Int,
) : Comparable<UnrealEngineVersion> {
    companion object {
        val empty = UnrealEngineVersion(0, 0, 0)
    }

    override fun compareTo(other: UnrealEngineVersion): Int =
        if (major != other.major) {
            major - other.major
        } else if (minor != other.minor) {
            minor - other.minor
        } else {
            patch - other.patch
        }

    override fun toString() = "$major.$minor.$patch"
}

@JvmInline
value class UnrealProjectPath(
    val value: String,
)

@JvmInline
value class UnrealEngineRootPath(
    val value: String,
)

data class UnrealEngineProject(
    val location: UnrealProjectPath,
    val engineIdentifier: UnrealEngineIdentifier?,
    val targetPlatforms: Collection<UnrealTargetPlatform>,
)
