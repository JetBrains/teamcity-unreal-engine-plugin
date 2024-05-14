package com.jetbrains.teamcity.plugins.unrealengine.common

@JvmInline
value class UnrealTargetPlatform(val value: String) {
    companion object {
        val Android = UnrealTargetPlatform("Android")
        val HoloLens = UnrealTargetPlatform("HoloLens")
        val IOS = UnrealTargetPlatform("IOS")
        val Linux = UnrealTargetPlatform("Linux")
        val LinuxArm64 = UnrealTargetPlatform("LinuxArm64")
        val Mac = UnrealTargetPlatform("Mac")
        val TVOS = UnrealTargetPlatform("TVOS")
        val Win64 = UnrealTargetPlatform("Win64")

        val knownPlatforms = setOf(
            Android,
            HoloLens,
            IOS,
            Linux,
            LinuxArm64,
            Mac,
            TVOS,
            Win64,
        )
    }
}
